package com.tkb.backend.solver;

import com.tkb.common.dto.PlanDto;
import com.tkb.common.dto.PlanDto.DroppedCourseDto;
import com.tkb.common.dto.PlanDto.MetricsDto;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * B6.1, B6.2, B6.3, B6.4, B6.5 - TimetableSolver: Thuat toan tim kiem va toi uu thoi khoa bieu.
 *
 * Cac ky thuat cot loi:
 *   1. Bitmask Lich hoc + Tuan hoc (B6.1):
 *      - Moi ngay (7 ngay: Thu 2..Chu Nhat) x 12 tiet duoc ma hoa thanh mang weekMask[7][12] (62-bit tuan hoc)
 *        kem bitmask 12-bit dayPeriodMask[7] de kiem tra xung dot nhanh O(1).
 *      - Hai buoi hoc cung thu va giao nhau ve tiet CHI trung nhau neu co it nhat 1 tuan hoc chung:
 *        (maskA & maskB) != 0L.
 *   2. Tach to hop Ly thuyet (LT) + To Thuc hanh (TH) (B6.2):
 *      - Neu 1 nhom lop (nhom) trong CSDL chua nhieu to thuc hanh (to_th = "01", "02", "03"... nhu mon 849010),
 *        thuat toan tu dong tach thanh cac phuong an ung vien (LT + to 01), (LT + to 02), (LT + to 03)
 *        de sinh vien chi hoc dung 1 to TH va van giu nguyen nhan LT/TH tren tung buoi.
 *   3. Ha bac uu tien thong minh khi trung lich (B6.3 - Yeu cau A.6):
 *      - Neu khong the xep du tat ca hoc phan, thuat toan xet theo thu tu uu tien giam dan:
 *        giu cac mon uu tien cao nhat va chi loai hoc phan co do uu tien thap nhat DANG gay xung dot
 *        (giu lai duoc cac hoc phan uu tien thap hon nhung khong xung dot).
 *   4. Cham diem 3 tieu chi & Xep hang Top N (B6.4, B6.5):
 *      - minDays (it ngay den truong), morningFirst (uu tien tiet sang 1-5), avoidGaps (tranh tiet trong giua buoi).
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/solver/TimetableSolver.java
 */
public class TimetableSolver {

    public static final int DAYS_COUNT = 7;          // Index 0..6 tuong ung Thu 2 (2) .. Chu Nhat (8)
    public static final int MAX_PERIODS = 12;        // Tiet 1..12 (index 0..11)
    public static final int MORNING_END_PERIOD = 5;  // Quy uoc A4: Tiet 1-5 la tiet buoi sang

    /** Gioi han thoi gian va so luong phuong an thu thap de tranh no to hop (B6.5) */
    private static final long TIME_LIMIT_MS = 8_000L;
    private static final int MAX_RAW_SOLUTIONS = 5_000;

    /**
     * Tieu chi toi uu do nguoi dung chon (ho tro ca trong so cho B10.1 diem cong).
     */
    public record Criteria(
            boolean minDays,
            boolean morningFirst,
            boolean avoidGaps,
            double weightMinDays,
            double weightMorning,
            double weightNoGaps
    ) {
        public Criteria(boolean minDays, boolean morningFirst, boolean avoidGaps) {
            this(minDays, morningFirst, avoidGaps, 1.0, 1.0, 1.0);
        }
    }

    /**
     * Ket qua tra ve cua thuat toan OPTIMIZE.
     */
    public record SolveOutcome(
            List<PlanDto> plans,
            List<DroppedCourseDto> droppedCourses,
            boolean cancelled
    ) {
    }

    /**
     * Ham chinh: Tim va xep hang cac phuong an thoi khoa bieu toi uu.
     *
     * @param orderedCourseCodes Danh sach ma hoc phan sap xep theo do quan trong GIAM DAN (dau danh sach la quan trong nhat)
     * @param sectionsByCourse   Map (courseCode -> danh sach SectionDto dang hoat dong cua hoc phan do)
     * @param criteria           Tieu chi toi uu (minDays, morningFirst, avoidGaps)
     * @param maxPlans           So luong phuong an toi da tra ve (1..10)
     * @param isCancelled        Ham kiem tra co volatile huy job (CANCEL_JOB hoac mat ket noi)
     * @param onProgress         Callback bao phan tram tien do (0..100) cho JOB_PROGRESS
     */
    public SolveOutcome solve(
            List<String> orderedCourseCodes,
            Map<String, List<SectionDto>> sectionsByCourse,
            Criteria criteria,
            int maxPlans,
            BooleanSupplier isCancelled,
            IntConsumer onProgress) {

        BooleanSupplier cancelCheck = (isCancelled != null) ? isCancelled : () -> false;
        IntConsumer progressReport = (onProgress != null) ? onProgress : p -> {};

        progressReport.accept(5);

        // 1. Bien dich danh sach nhom lop cua tung hoc phan thanh cac CompiledCandidate (tich hop Bitmask + tach to TH)
        Map<String, List<CompiledCandidate>> candidatesByCourse = new LinkedHashMap<>();
        Map<String, String> courseNames = new LinkedHashMap<>();

        for (String courseCode : orderedCourseCodes) {
            List<SectionDto> rawSections = sectionsByCourse.getOrDefault(courseCode, Collections.emptyList());
            if (!rawSections.isEmpty() && rawSections.get(0).getCourseName() != null) {
                courseNames.put(courseCode, rawSections.get(0).getCourseName());
            } else {
                courseNames.put(courseCode, courseCode);
            }
            List<CompiledCandidate> compiled = compileCourseCandidates(rawSections);
            candidatesByCourse.put(courseCode, compiled);
        }

        if (cancelCheck.getAsBoolean()) {
            return new SolveOutcome(Collections.emptyList(), Collections.emptyList(), true);
        }
        progressReport.accept(15);

        long deadlineMs = System.currentTimeMillis() + TIME_LIMIT_MS;

        // 2. Thu xep tat ca hoc phan truoc; neu trung lich -> ha bac loai hoc phan uu tien thap nhat gay xung dot (B6.3 / A.6)
        List<String> acceptedCourses = new ArrayList<>();
        List<DroppedCourseDto> droppedCourses = new ArrayList<>();
        List<List<SectionDto>> feasibleCombinations = Collections.emptyList();

        // Duyet theo thu tu uu tien tu cao xuong thap: thu them tung mon vao tap acceptedCourses.
        // Neu them mon thu i gay vo nghiem voi cac mon uu tien cao hon da chon -> mon thu i chinh la
        // hoc phan co do uu tien thap nhat trong nhom xung dot hien tai -> loai mon thu i va tiep tuc thu mon i+1!
        for (int i = 0; i < orderedCourseCodes.size(); i++) {
            if (cancelCheck.getAsBoolean()) {
                return new SolveOutcome(Collections.emptyList(), droppedCourses, true);
            }

            String candidateCourse = orderedCourseCodes.get(i);
            List<String> trialList = new ArrayList<>(acceptedCourses);
            trialList.add(candidateCourse);

            List<List<SectionDto>> trialSolutions = findFeasibleCombinations(
                    trialList, candidatesByCourse, cancelCheck, deadlineMs
            );

            if (!trialSolutions.isEmpty()) {
                acceptedCourses = trialList;
                feasibleCombinations = trialSolutions;
            } else {
                // candidateCourse gay xung dot voi cac mon uu tien cao hon -> bi loai
                droppedCourses.add(new DroppedCourseDto(
                        candidateCourse,
                        courseNames.getOrDefault(candidateCourse, candidateCourse)
                ));
            }

            int pct = 15 + (int) (((i + 1) * 60.0) / Math.max(1, orderedCourseCodes.size()));
            progressReport.accept(Math.min(75, pct));
        }

        if (cancelCheck.getAsBoolean()) {
            return new SolveOutcome(Collections.emptyList(), droppedCourses, true);
        }

        // 3. Cham diem moi phuong an theo 3 tieu chi va xep hang (B6.4, B6.5)
        List<PlanDto> scoredPlans = new ArrayList<>(feasibleCombinations.size());
        for (List<SectionDto> combo : feasibleCombinations) {
            if (cancelCheck.getAsBoolean()) {
                return new SolveOutcome(Collections.emptyList(), droppedCourses, true);
            }
            PlanDto plan = evaluateAndBuildPlan(combo, criteria, droppedCourses);
            scoredPlans.add(plan);
        }

        progressReport.accept(90);

        // Sap xep giam dan theo score; neu bang diem thi uu tien it ngay hoc hon -> it tiet trong hon -> nhieu tiet sang hon
        scoredPlans.sort(
                Comparator.comparingDouble(PlanDto::getScore).reversed()
                        .thenComparingInt(p -> p.getMetrics().getStudyDays())
                        .thenComparingInt(p -> p.getMetrics().getGapPeriods())
                        .thenComparing(Comparator.comparingInt((PlanDto p) -> p.getMetrics().getMorningPeriods()).reversed())
        );

        int limit = Math.max(1, Math.min(maxPlans, scoredPlans.size()));
        List<PlanDto> topPlans = new ArrayList<>(limit);
        for (int idx = 0; idx < limit; idx++) {
            PlanDto plan = scoredPlans.get(idx);
            plan.setRank(idx + 1);
            topPlans.add(plan);
        }

        progressReport.accept(100);
        return new SolveOutcome(topPlans, droppedCourses, false);
    }

    // =========================================================================
    // HAM KIEM TRA TRUNG GIO & CHAM DIEM (DUNG CHUNG CHO CA OPTIMIZE VA SAVE_PLAN B8.1)
    // =========================================================================

    /**
     * Kiem tra danh sach SectionDto co bat ky 2 buoi hoc nao trung gio (cung thu, giao tiet, giao tuan) hay khong.
     */
    public boolean hasAnyConflict(List<SectionDto> sections) {
        if (sections == null || sections.size() <= 1) {
            return false;
        }
        long[][] board = new long[DAYS_COUNT][MAX_PERIODS];
        for (SectionDto sec : sections) {
            if (sec.getSlots() == null) continue;
            for (SlotDto slot : sec.getSlots()) {
                int d = slot.getDayOfWeek() - 2;
                if (d < 0 || d >= DAYS_COUNT) continue;
                int startP = Math.max(1, slot.getStartPeriod());
                int endP = Math.min(MAX_PERIODS, slot.getEndPeriod());
                long wMask = slot.getWeekMask();
                for (int p = startP - 1; p < endP; p++) {
                    if ((board[d][p] & wMask) != 0L) {
                        return true;
                    }
                    board[d][p] |= wMask;
                }
            }
        }
        return false;
    }

    /**
     * Tinh toan MetricsDto (studyDays, gapPeriods, morningPeriods) va diem score (thang 100)
     * cho mot danh sach SectionDto.
     */
    public PlanDto evaluateAndBuildPlan(
            List<SectionDto> sections,
            Criteria criteria,
            List<DroppedCourseDto> droppedCourses) {

        boolean[][] occupied = new boolean[DAYS_COUNT][MAX_PERIODS];
        int totalPeriods = 0;
        int morningPeriods = 0;

        for (SectionDto sec : sections) {
            if (sec.getSlots() == null) continue;
            for (SlotDto slot : sec.getSlots()) {
                int d = slot.getDayOfWeek() - 2;
                if (d < 0 || d >= DAYS_COUNT) continue;
                int startP = Math.max(1, slot.getStartPeriod());
                int endP = Math.min(MAX_PERIODS, slot.getEndPeriod());
                for (int p = startP; p <= endP; p++) {
                    if (!occupied[d][p - 1]) {
                        occupied[d][p - 1] = true;
                        totalPeriods++;
                        if (p <= MORNING_END_PERIOD) {
                            morningPeriods++;
                        }
                    }
                }
            }
        }

        int studyDays = 0;
        int gapPeriods = 0;

        for (int d = 0; d < DAYS_COUNT; d++) {
            int firstPeriod = -1;
            int lastPeriod = -1;
            int learnedInDay = 0;
            for (int p = 0; p < MAX_PERIODS; p++) {
                if (occupied[d][p]) {
                    if (firstPeriod == -1) {
                        firstPeriod = p;
                    }
                    lastPeriod = p;
                    learnedInDay++;
                }
            }
            if (learnedInDay > 0) {
                studyDays++;
                int span = (lastPeriod - firstPeriod + 1);
                gapPeriods += Math.max(0, span - learnedInDay);
            }
        }

        MetricsDto metrics = new MetricsDto(studyDays, gapPeriods, morningPeriods);
        double score = computeScore(metrics, totalPeriods, criteria);

        return new PlanDto(1, score, metrics, new ArrayList<>(sections), droppedCourses);
    }

    /**
     * B6.4 - Ham cham diem phuong an tren thang diem 0..100:
     *   - It ngay den truong (minDays): 1 ngay = 100đ, 7 ngay = 0đ -> (7 - studyDays) / 6.0 * 100
     *   - Uu tien buoi sang (morningFirst): ty le tiet sang / tong so tiet * 100
     *   - Tranh tiet trong (avoidGaps): 0 tiet trong = 100đ, moi tiet trong tru 12.5đ
     */
    public double computeScore(MetricsDto metrics, int totalPeriods, Criteria criteria) {
        Criteria c = (criteria != null) ? criteria : new Criteria(true, true, true);

        double dayScore = (metrics.getStudyDays() <= 0)
                ? 100.0
                : Math.max(0.0, Math.min(100.0, ((7.0 - metrics.getStudyDays()) / 6.0) * 100.0));

        double morningScore = (totalPeriods <= 0)
                ? 100.0
                : ((double) metrics.getMorningPeriods() / totalPeriods) * 100.0;

        double gapScore = Math.max(0.0, 100.0 - (metrics.getGapPeriods() * 12.5));

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        if (c.minDays()) {
            weightedSum += dayScore * c.weightMinDays();
            totalWeight += c.weightMinDays();
        }
        if (c.morningFirst()) {
            weightedSum += morningScore * c.weightMorning();
            totalWeight += c.weightMorning();
        }
        if (c.avoidGaps()) {
            weightedSum += gapScore * c.weightNoGaps();
            totalWeight += c.weightNoGaps();
        }

        // Neu nguoi dung khong tick tieu chi nao -> tinh trung binh deu ca 3 tieu chi
        if (totalWeight <= 0.0) {
            weightedSum = dayScore + morningScore + gapScore;
            totalWeight = 3.0;
        }

        double rawScore = weightedSum / totalWeight;
        return Math.round(rawScore * 100.0) / 100.0;
    }

    // =========================================================================
    // BACKTRACKING + BITMASK PRUNING NOI BO
    // =========================================================================

    private List<List<SectionDto>> findFeasibleCombinations(
            List<String> courseList,
            Map<String, List<CompiledCandidate>> candidatesByCourse,
            BooleanSupplier cancelCheck,
            long deadlineMs) {

        for (String c : courseList) {
            List<CompiledCandidate> list = candidatesByCourse.get(c);
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
        }

        // Sap xep thu tu duyet Backtracking theo nguyen tac MRV (Most Constrained Variable):
        // Hoc phan nao co IT nhom lop ung vien nhat duoc duyet truoc de cat tia nhanh nhat!
        List<String> mrvOrder = new ArrayList<>(courseList);
        mrvOrder.sort(Comparator.comparingInt(c -> candidatesByCourse.get(c).size()));

        long[][] boardWeekMask = new long[DAYS_COUNT][MAX_PERIODS];
        int[] boardDayMask = new int[DAYS_COUNT];
        Map<String, SectionDto> currentPick = new LinkedHashMap<>();
        List<List<SectionDto>> results = new ArrayList<>();

        backtrack(
                0,
                mrvOrder,
                courseList,
                candidatesByCourse,
                boardWeekMask,
                boardDayMask,
                currentPick,
                results,
                cancelCheck,
                deadlineMs
        );

        return results;
    }

    private void backtrack(
            int depth,
            List<String> mrvOrder,
            List<String> originalOrder,
            Map<String, List<CompiledCandidate>> candidatesByCourse,
            long[][] boardWeekMask,
            int[] boardDayMask,
            Map<String, SectionDto> currentPick,
            List<List<SectionDto>> results,
            BooleanSupplier cancelCheck,
            long deadlineMs) {

        if (results.size() >= MAX_RAW_SOLUTIONS || cancelCheck.getAsBoolean()) {
            return;
        }
        if (System.currentTimeMillis() > deadlineMs && !results.isEmpty()) {
            return;
        }

        if (depth == mrvOrder.size()) {
            List<SectionDto> orderedCombo = new ArrayList<>(originalOrder.size());
            for (String code : originalOrder) {
                orderedCombo.add(currentPick.get(code));
            }
            results.add(orderedCombo);
            return;
        }

        String courseCode = mrvOrder.get(depth);
        List<CompiledCandidate> candidates = candidatesByCourse.get(courseCode);

        for (CompiledCandidate cand : candidates) {
            if ( canPlace(cand, boardWeekMask, boardDayMask)) {
                place(cand, boardWeekMask, boardDayMask);
                currentPick.put(courseCode, cand.sectionDto);

                backtrack(
                        depth + 1,
                        mrvOrder,
                        originalOrder,
                        candidatesByCourse,
                        boardWeekMask,
                        boardDayMask,
                        currentPick,
                        results,
                        cancelCheck,
                        deadlineMs
                );

                currentPick.remove(courseCode);
                unplace(cand, boardWeekMask, boardDayMask);

                if (results.size() >= MAX_RAW_SOLUTIONS || cancelCheck.getAsBoolean()) {
                    return;
                }
            }
        }
    }

    private boolean canPlace(CompiledCandidate cand, long[][] boardWeekMask, int[] boardDayMask) {
        for (int d = 0; d < DAYS_COUNT; d++) {
            // Kiem tra nhanh bang bitmask 12 tiet trong ngay truoc
            if ((boardDayMask[d] & cand.dayPeriodMask[d]) == 0) {
                continue;
            }
            // Neu co tiet giao nhau trong ngay d -> kiem tra xem co trung tuan hoc (weekMask) khong
            for (int p = 0; p < MAX_PERIODS; p++) {
                long candW = cand.weekMask[d][p];
                if (candW != 0L && (boardWeekMask[d][p] & candW) != 0L) {
                    return false;
                }
            }
        }
        return true;
    }

    private void place(CompiledCandidate cand, long[][] boardWeekMask, int[] boardDayMask) {
        for (int d = 0; d < DAYS_COUNT; d++) {
            if (cand.dayPeriodMask[d] == 0) continue;
            boardDayMask[d] |= cand.dayPeriodMask[d];
            for (int p = 0; p < MAX_PERIODS; p++) {
                boardWeekMask[d][p] |= cand.weekMask[d][p];
            }
        }
    }

    private void unplace(CompiledCandidate cand, long[][] boardWeekMask, int[] boardDayMask) {
        for (int d = 0; d < DAYS_COUNT; d++) {
            if (cand.dayPeriodMask[d] == 0) continue;
            int newDayMask = 0;
            for (int p = 0; p < MAX_PERIODS; p++) {
                boardWeekMask[d][p] &= ~cand.weekMask[d][p];
                if (boardWeekMask[d][p] != 0L) {
                    newDayMask |= (1 << p);
                }
            }
            boardDayMask[d] = newDayMask;
        }
    }

    /**
     * Bien dich danh sach SectionDto cua 1 hoc phan thanh cac CompiledCandidate.
     * Xu ly thong minh truong hop 1 nhom_lop chua nhieu to thuc hanh (to_th = "01", "02", "03"):
     *   - Tach cac buoi LT chung + tung to TH rieng thanh cac ung vien SectionDto doc lap
     *     de sinh vien chi hoc dung 1 to TH ma van giu nguyen LT + TH.
     */
    private List<CompiledCandidate> compileCourseCandidates(List<SectionDto> rawSections) {
        List<CompiledCandidate> result = new ArrayList<>();
        for (SectionDto sec : rawSections) {
            if (!sec.isActive() || sec.getSlots() == null || sec.getSlots().isEmpty()) {
                continue;
            }

            List<SlotDto> ltSlots = new ArrayList<>();
            Map<String, List<SlotDto>> thSlotsByLabGroup = new LinkedHashMap<>();

            for (SlotDto slot : sec.getSlots()) {
                if (SectionDto.TYPE_TH.equalsIgnoreCase(slot.getType())
                        && slot.getLabGroup() != null
                        && !slot.getLabGroup().isBlank()) {
                    thSlotsByLabGroup.computeIfAbsent(slot.getLabGroup().trim(), k -> new ArrayList<>()).add(slot);
                } else {
                    ltSlots.add(slot);
                }
            }

            if (thSlotsByLabGroup.size() <= 1) {
                // Nhom lop chi co LT hoac chi co 1 to TH duy nhat -> giu nguyen
                CompiledCandidate cand = new CompiledCandidate(sec);
                if (!cand.selfConflicting) {
                    result.add(cand);
                }
            } else {
                // Nhom lop co nhieu to TH (vd: to 01, 02, 03) -> tao ung vien cho tung (LT + to TH)
                for (Map.Entry<String, List<SlotDto>> entry : thSlotsByLabGroup.entrySet()) {
                    String labGroup = entry.getKey();
                    List<SlotDto> combinedSlots = new ArrayList<>(ltSlots.size() + entry.getValue().size());
                    combinedSlots.addAll(ltSlots);
                    combinedSlots.addAll(entry.getValue());

                    SectionDto variant = new SectionDto(
                            sec.getId(),
                            sec.getCourseCode(),
                            sec.getCourseName(),
                            sec.getCredits(),
                            sec.getGroupNo(),
                            labGroup,
                            SectionDto.TYPE_TH,
                            sec.getLecturer(),
                            sec.getLecturerCode(),
                            sec.getClassName(),
                            sec.getCapacity(),
                            sec.isActive(),
                            combinedSlots
                    );
                    variant.setSemesterCode(sec.getSemesterCode());
                    CompiledCandidate cand = new CompiledCandidate(variant);
                    if (!cand.selfConflicting) {
                        result.add(cand);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Ung vien nhom lop da duoc ma hoa Bitmask san sang cho Backtracking.
     */
    private static final class CompiledCandidate {
        final SectionDto sectionDto;
        final long[][] weekMask = new long[DAYS_COUNT][MAX_PERIODS];
        final int[] dayPeriodMask = new int[DAYS_COUNT];
        final boolean selfConflicting;

        CompiledCandidate(SectionDto sectionDto) {
            this.sectionDto = sectionDto;
            boolean conflict = false;
            if (sectionDto.getSlots() != null) {
                for (SlotDto slot : sectionDto.getSlots()) {
                    int d = slot.getDayOfWeek() - 2;
                    if (d < 0 || d >= DAYS_COUNT) continue;
                    int startP = Math.max(1, slot.getStartPeriod());
                    int endP = Math.min(MAX_PERIODS, slot.getEndPeriod());
                    long wMask = slot.getWeekMask();

                    for (int p = startP - 1; p < endP; p++) {
                        if ((weekMask[d][p] & wMask) != 0L) {
                            conflict = true;
                        }
                        weekMask[d][p] |= wMask;
                        dayPeriodMask[d] |= (1 << p);
                    }
                }
            }
            this.selfConflicting = conflict;
        }
    }
}