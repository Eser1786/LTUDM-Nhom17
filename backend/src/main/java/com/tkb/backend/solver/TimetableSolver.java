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
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * B6.1 -> B6.5 & B10.1, B10.2 - TimetableSolver (Phien ban nang cap):
 * Ho tro day du mo hinh 2 Khung keo tha (Drag & Drop Priority):
 *
 *   - Khung 1 (Thu tu uu tien Mon hoc & Bo loc tren tung mon):
 *       + Danh sach mon hoc sap tu tren xuong (mon o tren duoc giu truoc, mon o duoi gay xung dot bi loai).
 *       + Tuy chon uu tien Giang vien (preferredLecturers) hoac Khoa nhom lop (lockedGroups) tren tung mon.
 *
 *   - Khung 2 (Thu tu uu tien Tieu chi & Trong so tu dong theo vi tri keo tha):
 *       1. So ngay di hoc (minDays) + Thanh chon cac ngay muon hoc (preferredDays: Thu 2..CN).
 *       2. Khung gio hoc (timeMode):
 *          - "MORNING": Uu tien buoi sang (tiet 1-5)
 *          - "AFTERNOON": Uu tien buoi chieu (tiet 6-10)
 *          - "CUSTOM_MATRIX": Ma tran 7 ngay x 12 tiet (preferredSlotsMask[7]) do sinh vien quet chon.
 *            Tra ve phuong an co ty le tiet nam trong vung da chon cao nhat (bang gan nhat).
 *       3. Co so hoc (preferredCampus: "C", "1", "2") & Tranh 2 tiet ke nhau khac co so (avoidCampusSwitch):
 *          - Tach ky tu dau truoc dau '.' cua ma phong (vd: "C.A103" -> "C", "1.A103" -> "1", "2.A103" -> "2").
 *          - Phat nang neu 2 tiet lien ke nhau trong cung 1 ngay nam o 2 co so khac nhau (sinh vien khong kip chay co so).
 *       4. Tranh tiet trong giua buoi (avoidGaps): Gom cac tiet trong cung 1 ngay lien mach nhau.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/solver/TimetableSolver.java
 */
public class TimetableSolver {

    public static final int DAYS_COUNT = 7;          // Index 0..6 tuong ung Thu 2 (2) .. Chu Nhat (8)
    public static final int MAX_PERIODS = 12;        // Tiet 1..12 (index 0..11)
    public static final int MORNING_END_PERIOD = 5;  // Tiet 1-5 la buoi sang
    public static final int AFTERNOON_END_PERIOD = 10; // Tiet 6-10 la buoi chieu

    private static final long TIME_LIMIT_MS = 8_000L;
    private static final int MAX_RAW_SOLUTIONS = 5_000;

    /**
     * Cau hinh Tieu chi toi uu (tuong thich nguoc 100% voi Protocol cu va mo rong cho 2 Khung keo tha).
     */
    public record Criteria(
            boolean minDays,
            boolean morningFirst,
            boolean avoidGaps,
            double weightMinDays,
            double weightMorning,
            double weightNoGaps,
            // --- Cac truong mo rong cho Khung 1 & Khung 2 ---
            String timeMode,                 // "MORNING", "AFTERNOON", "CUSTOM_MATRIX"
            Set<Integer> preferredDays,      // Tap cac thu muon hoc (2..8), rong = moi ngay
            int[] preferredSlotsMask,        // Mang 7 phan tu (Thu 2..CN), moi phan tu la bitmask 12 tiet muon hoc
            String preferredCampus,          // "C" (Co so chinh), "1" (Co so 1), "2" (Co so 2), hoac "" (Tat ca)
            boolean avoidCampusSwitch,       // Tranh 2 tiet ke nhau / cung ngay nhung khac co so
            double weightCampus,             // Trong so tieu chi Co so (theo vi tri keo tha)
            Map<String, String> preferredLecturers, // Map ma_mh -> ten giang vien uu tien (Khung 1)
            Map<String, String> lockedGroups        // Map ma_mh -> ma nhom khoa cung (Khung 1)
    ) {
        /** Constructor 3 tham so co ban */
        public Criteria(boolean minDays, boolean morningFirst, boolean avoidGaps) {
            this(minDays, morningFirst, avoidGaps, 1.0, 1.0, 1.0);
        }

        /** Constructor 6 tham so (tuong thich voi code cu) */
        public Criteria(
                boolean minDays,
                boolean morningFirst,
                boolean avoidGaps,
                double weightMinDays,
                double weightMorning,
                double weightNoGaps) {
            this(
                    minDays,
                    morningFirst,
                    avoidGaps,
                    weightMinDays,
                    weightMorning,
                    weightNoGaps,
                    morningFirst ? "MORNING" : "NONE",
                    Collections.emptySet(),
                    null,
                    "",
                    true,
                    1.0,
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }
    }

    public record SolveOutcome(
            List<PlanDto> plans,
            List<DroppedCourseDto> droppedCourses,
            boolean cancelled
    ) {
    }

    /**
     * Ham chinh: Tim va xep hang cac phuong an thoi khoa bieu toi uu.
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
        Criteria activeCriteria = (criteria != null) ? criteria : new Criteria(true, true, true);

        progressReport.accept(5);

        // 1. Bien dich danh sach nhom lop cua tung hoc phan thanh cac CompiledCandidate
        Map<String, List<CompiledCandidate>> candidatesByCourse = new LinkedHashMap<>();
        Map<String, String> courseNames = new LinkedHashMap<>();

        for (String courseCode : orderedCourseCodes) {
            List<SectionDto> rawSections = sectionsByCourse.getOrDefault(courseCode, Collections.emptyList());
            if (!rawSections.isEmpty() && rawSections.get(0).getCourseName() != null) {
                courseNames.put(courseCode, rawSections.get(0).getCourseName());
            } else {
                courseNames.put(courseCode, courseCode);
            }

            String lockedGroup = (activeCriteria.lockedGroups() != null)
                    ? activeCriteria.lockedGroups().get(courseCode)
                    : null;
            List<CompiledCandidate> compiled = compileCourseCandidates(rawSections, lockedGroup);
            candidatesByCourse.put(courseCode, compiled);
        }

        if (cancelCheck.getAsBoolean()) {
            return new SolveOutcome(Collections.emptyList(), Collections.emptyList(), true);
        }
        progressReport.accept(15);

        long deadlineMs = System.currentTimeMillis() + TIME_LIMIT_MS;

        // 2. Xep theo thu tu uu tien tu cao xuong thap (Khung 1) va ha bac khi trung lich (Yeu cau A.6)
        List<String> acceptedCourses = new ArrayList<>();
        List<DroppedCourseDto> droppedCourses = new ArrayList<>();
        List<List<SectionDto>> feasibleCombinations = Collections.emptyList();

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

        // 3. Cham diem moi phuong an theo cac tieu chi o Khung 2 & xep hang
        List<PlanDto> scoredPlans = new ArrayList<>(feasibleCombinations.size());
        for (List<SectionDto> combo : feasibleCombinations) {
            if (cancelCheck.getAsBoolean()) {
                return new SolveOutcome(Collections.emptyList(), droppedCourses, true);
            }
            PlanDto plan = evaluateAndBuildPlan(combo, activeCriteria, droppedCourses);
            scoredPlans.add(plan);
        }

        progressReport.accept(90);

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
    // KIEM TRA TRUNG GIO, PHAN TICH CO SO & CHAM DIEM CHI TIET
    // =========================================================================

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
     * Tach ma Co so tu ma phong cua SGU (vd: "C.A103" -> "C", "1.A103" -> "1", "2.A103" -> "2").
     */
    public static String extractCampusCode(String room) {
        if (room == null || room.isBlank()) {
            return "";
        }
        String r = room.trim().toUpperCase();
        int dotIdx = r.indexOf('.');
        if (dotIdx > 0) {
            return r.substring(0, dotIdx).trim();
        }
        return String.valueOf(r.charAt(0));
    }

    /**
     * Tinh toan MetricsDto (studyDays, gapPeriods, morningPeriods) va cham diem tong hop
     * dua tren:
     *   1. So ngay di hoc & Thanh chon ngay muon hoc (preferredDays).
     *   2. Khung gio mong muon (Sang / Chieu / Ma tran 7x12 tiet preferredSlotsMask).
     *   3. Co so uu tien (C, 1, 2) & Tranh 2 tiet ke nhau khac co so (avoidCampusSwitch).
     *   4. Tranh tiet trong giua buoi (avoidGaps).
     *   5. Giang vien uu tien tren tung mon (preferredLecturers).
     */
    public PlanDto evaluateAndBuildPlan(
            List<SectionDto> sections,
            Criteria criteria,
            List<DroppedCourseDto> droppedCourses) {

        Criteria c = (criteria != null) ? criteria : new Criteria(true, true, true);

        boolean[][] occupied = new boolean[DAYS_COUNT][MAX_PERIODS];
        String[][] campusGrid = new String[DAYS_COUNT][MAX_PERIODS];

        int totalPeriods = 0;
        int morningPeriods = 0;
        int afternoonPeriods = 0;
        int customMatrixMatchedPeriods = 0;
        int preferredDayMatchedPeriods = 0;
        int preferredCampusPeriods = 0;
        int campusTrackedPeriods = 0;

        String targetCampus = (c.preferredCampus() != null) ? c.preferredCampus().trim().toUpperCase() : "";
        boolean hasPreferredDays = c.preferredDays() != null && !c.preferredDays().isEmpty();
        int[] customMask = c.preferredSlotsMask();

        int matchedLecturers = 0;
        int totalLecturerPrefs = 0;

        for (SectionDto sec : sections) {
            // Kiem tra uu tien Giang vien tren Khung 1 (neu co)
            if (c.preferredLecturers() != null && sec.getCourseCode() != null) {
                String wantedLec = c.preferredLecturers().get(sec.getCourseCode());
                if (wantedLec != null && !wantedLec.isBlank()) {
                    totalLecturerPrefs++;
                    if (sec.getLecturer() != null
                            && sec.getLecturer().toLowerCase().contains(wantedLec.trim().toLowerCase())) {
                        matchedLecturers++;
                    }
                }
            }

            if (sec.getSlots() == null) continue;
            for (SlotDto slot : sec.getSlots()) {
                int dayOfWeek = slot.getDayOfWeek();
                int d = dayOfWeek - 2;
                if (d < 0 || d >= DAYS_COUNT) continue;

                String slotCampus = extractCampusCode(slot.getRoom());
                int startP = Math.max(1, slot.getStartPeriod());
                int endP = Math.min(MAX_PERIODS, slot.getEndPeriod());

                for (int p = startP; p <= endP; p++) {
                    int pIdx = p - 1;
                    if (!occupied[d][pIdx]) {
                        occupied[d][pIdx] = true;
                        campusGrid[d][pIdx] = slotCampus;
                        totalPeriods++;

                        if (p <= MORNING_END_PERIOD) {
                            morningPeriods++;
                        } else if (p <= AFTERNOON_END_PERIOD) {
                            afternoonPeriods++;
                        }

                        if (hasPreferredDays && c.preferredDays().contains(dayOfWeek)) {
                            preferredDayMatchedPeriods++;
                        }

                        if (customMask != null && d < customMask.length) {
                            if ((customMask[d] & (1 << pIdx)) != 0) {
                                customMatrixMatchedPeriods++;
                            }
                        }

                        if (!slotCampus.isEmpty()) {
                            campusTrackedPeriods++;
                            if (!targetCampus.isEmpty() && targetCampus.equals(slotCampus)) {
                                preferredCampusPeriods++;
                            }
                        }
                    }
                }
            }
        }

        int studyDays = 0;
        int gapPeriods = 0;
        int adjacentCampusSwitches = 0; // So lan 2 tiet ke sat nhau (hoac cach 1 tiet) ma khac co so!
        int sameDayCampusSwitches = 0;  // So lan doi co so trong cung 1 ngay

        for (int d = 0; d < DAYS_COUNT; d++) {
            int firstPeriod = -1;
            int lastPeriod = -1;
            int learnedInDay = 0;
            String prevCampus = "";
            int prevPeriodIdx = -1;

            for (int p = 0; p < MAX_PERIODS; p++) {
                if (occupied[d][p]) {
                    if (firstPeriod == -1) {
                        firstPeriod = p;
                    }
                    lastPeriod = p;
                    learnedInDay++;

                    String curCampus = campusGrid[d][p] != null ? campusGrid[d][p] : "";
                    if (!curCampus.isEmpty()) {
                        if (!prevCampus.isEmpty() && !prevCampus.equals(curCampus)) {
                            // Phat hien doi co so trong cung 1 ngay!
                            if (p - prevPeriodIdx <= 2) {
                                // Hai tiet ke sat nhau (vd: tiet 3 sang tiet 4, hoac tiet 5 sang tiet 6) -> Phai chay co so!
                                adjacentCampusSwitches++;
                            } else {
                                sameDayCampusSwitches++;
                            }
                        }
                        prevCampus = curCampus;
                        prevPeriodIdx = p;
                    }
                }
            }
            if (learnedInDay > 0) {
                studyDays++;
                int span = (lastPeriod - firstPeriod + 1);
                gapPeriods += Math.max(0, span - learnedInDay);
            }
        }

        MetricsDto metrics = new MetricsDto(studyDays, gapPeriods, morningPeriods);
        double score = computeExtendedScore(
                metrics,
                totalPeriods,
                afternoonPeriods,
                customMatrixMatchedPeriods,
                preferredDayMatchedPeriods,
                preferredCampusPeriods,
                campusTrackedPeriods,
                adjacentCampusSwitches,
                sameDayCampusSwitches,
                matchedLecturers,
                totalLecturerPrefs,
                c
        );

        return new PlanDto(1, score, metrics, new ArrayList<>(sections), droppedCourses);
    }

    /**
     * Tinh diem tong hop tren thang 0..100 (giu nguyen tinh tuong thich voi computeScore cu).
     */
    public double computeScore(MetricsDto metrics, int totalPeriods, Criteria criteria) {
        return computeExtendedScore(
                metrics,
                totalPeriods,
                Math.max(0, totalPeriods - metrics.getMorningPeriods()),
                0,
                totalPeriods,
                0,
                0,
                0,
                0,
                0,
                0,
                criteria
        );
    }

    private double computeExtendedScore(
            MetricsDto metrics,
            int totalPeriods,
            int afternoonPeriods,
            int customMatrixMatchedPeriods,
            int preferredDayMatchedPeriods,
            int preferredCampusPeriods,
            int campusTrackedPeriods,
            int adjacentCampusSwitches,
            int sameDayCampusSwitches,
            int matchedLecturers,
            int totalLecturerPrefs,
            Criteria c) {

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        // 1. Tieu chi Ngay di hoc (minDays + preferredDays tren thanh Thu 2..CN)
        if (c.minDays() || (c.preferredDays() != null && !c.preferredDays().isEmpty())) {
            double compactnessScore = (metrics.getStudyDays() <= 0)
                    ? 100.0
                    : Math.max(0.0, Math.min(100.0, ((7.0 - metrics.getStudyDays()) / 6.0) * 100.0));

            double dayScore = compactnessScore;
            if (c.preferredDays() != null && !c.preferredDays().isEmpty() && totalPeriods > 0) {
                double matchDayRatio = ((double) preferredDayMatchedPeriods / totalPeriods) * 100.0;
                // Ket hop 70% do khop voi cac ngay da chon tren thanh ngang + 30% do gon so ngay
                dayScore = c.minDays() ? (0.7 * matchDayRatio + 0.3 * compactnessScore) : matchDayRatio;
            }
            double w = Math.max(0.1, c.weightMinDays());
            weightedSum += dayScore * w;
            totalWeight += w;
        }

        // 2. Tieu chi Khung gio hoc (MORNING / AFTERNOON / CUSTOM_MATRIX)
        String mode = (c.timeMode() != null) ? c.timeMode().trim().toUpperCase() : (c.morningFirst() ? "MORNING" : "NONE");
        if (c.morningFirst() || "MORNING".equals(mode) || "AFTERNOON".equals(mode) || "CUSTOM_MATRIX".equals(mode)) {
            double timeScore = 100.0;
            if (totalPeriods > 0) {
                if ("AFTERNOON".equals(mode)) {
                    timeScore = ((double) afternoonPeriods / totalPeriods) * 100.0;
                } else if ("CUSTOM_MATRIX".equals(mode) && c.preferredSlotsMask() != null) {
                    timeScore = ((double) customMatrixMatchedPeriods / totalPeriods) * 100.0;
                } else {
                    timeScore = ((double) metrics.getMorningPeriods() / totalPeriods) * 100.0;
                }
            }
            double w = Math.max(0.1, c.weightMorning());
            weightedSum += timeScore * w;
            totalWeight += w;
        }

        // 3. Tieu chi Tranh tiet trong giua buoi (avoidGaps)
        if (c.avoidGaps()) {
            double gapScore = Math.max(0.0, 100.0 - (metrics.getGapPeriods() * 12.5));
            double w = Math.max(0.1, c.weightNoGaps());
            weightedSum += gapScore * w;
            totalWeight += w;
        }

        // 4. Tieu chi Co so hoc (preferredCampus: C, 1, 2) & Tranh chay co so (avoidCampusSwitch)
        boolean hasCampusPref = c.preferredCampus() != null && !c.preferredCampus().isBlank() && !"ANY".equalsIgnoreCase(c.preferredCampus());
        if (hasCampusPref || c.avoidCampusSwitch()) {
            double campusMatchScore = 100.0;
            if (hasCampusPref && campusTrackedPeriods > 0) {
                campusMatchScore = ((double) preferredCampusPeriods / campusTrackedPeriods) * 100.0;
            }
            // Phat nang neu 2 tiet ke nhau khac co so (-45đ moi lan) hoac doi co so cung ngay (-20đ moi lan)
            double switchPenalty = c.avoidCampusSwitch()
                    ? (adjacentCampusSwitches * 45.0 + sameDayCampusSwitches * 20.0)
                    : 0.0;
            double campusScore = Math.max(0.0, campusMatchScore - switchPenalty);

            double w = Math.max(0.1, c.weightCampus());
            weightedSum += campusScore * w;
            totalWeight += w;
        }

        // 5. Diem thuong/khop Giang vien tren Khung 1 (neu sinh vien chon giang vien cho mon hoc)
        if (totalLecturerPrefs > 0) {
            double lecScore = ((double) matchedLecturers / totalLecturerPrefs) * 100.0;
            weightedSum += lecScore * 2.0;
            totalWeight += 2.0;
        }

        if (totalWeight <= 0.0) {
            double dayScore = Math.max(0.0, Math.min(100.0, ((7.0 - metrics.getStudyDays()) / 6.0) * 100.0));
            double morningScore = (totalPeriods <= 0) ? 100.0 : ((double) metrics.getMorningPeriods() / totalPeriods) * 100.0;
            double gapScore = Math.max(0.0, 100.0 - (metrics.getGapPeriods() * 12.5));
            weightedSum = dayScore + morningScore + gapScore;
            totalWeight = 3.0;
        }

        double finalScore = weightedSum / totalWeight;
        // Neu van con vi pham 2 tiet ke sat nhau khac co so (khi bat avoidCampusSwitch), tru them vao diem tong
        // de cac phuong an KHONG phai chay co so luon duoc xep len tren cung!
        if (c.avoidCampusSwitch() && adjacentCampusSwitches > 0) {
            finalScore = Math.max(0.0, finalScore - (adjacentCampusSwitches * 15.0));
        }

        return Math.round(finalScore * 100.0) / 100.0;
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
            if (canPlace(cand, boardWeekMask, boardDayMask)) {
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
            if ((boardDayMask[d] & cand.dayPeriodMask[d]) == 0) {
                continue;
            }
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

    private List<CompiledCandidate> compileCourseCandidates(List<SectionDto> rawSections, String lockedGroup) {
        List<CompiledCandidate> allCompiled = new ArrayList<>();
        List<CompiledCandidate> lockedCompiled = new ArrayList<>();

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
                CompiledCandidate cand = new CompiledCandidate(sec);
                if (!cand.selfConflicting) {
                    allCompiled.add(cand);
                    if (lockedGroup != null && !lockedGroup.isBlank() && lockedGroup.trim().equalsIgnoreCase(sec.getGroupNo())) {
                        lockedCompiled.add(cand);
                    }
                }
            } else {
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
                        allCompiled.add(cand);
                        if (lockedGroup != null && !lockedGroup.isBlank() && lockedGroup.trim().equalsIgnoreCase(sec.getGroupNo())) {
                            lockedCompiled.add(cand);
                        }
                    }
                }
            }
        }

        // Neu sinh vien khoa cung 1 nhom lop va nhom do ton tai -> uu tien dung nhom do
        return !lockedCompiled.isEmpty() ? lockedCompiled : allCompiled;
    }

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