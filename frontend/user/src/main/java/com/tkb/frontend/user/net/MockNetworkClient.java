package com.tkb.frontend.user.net;

import com.tkb.frontend.user.model.ErrorCode;
import com.tkb.frontend.user.model.Types.ClassSession;
import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.JobResult;
import com.tkb.frontend.user.model.Types.JobStatus;
import com.tkb.frontend.user.model.Types.OptionStats;
import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.model.Types.PlanRequest;
import com.tkb.frontend.user.model.Types.SavedPlan;
import com.tkb.frontend.user.model.Types.Section;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.model.Types.ScheduleOption;
import com.tkb.frontend.user.model.Types.Semester;
import com.tkb.frontend.user.model.Types.Student;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ban gia lap cua TkbClient dung trong giai doan phat trien giao dien user.
 * Du lieu mau duoc bien so de tao ra dung 3 phuong an A/B/C theo mau so:
 * A - 3 ngay, 0 phut trong, 100% buoi sang; B - 2 ngay, 240 phut, 67%;
 * C - 3 ngay, 120 phut, 83%.
 */
public class MockNetworkClient implements TkbClient {

    private static final String CURRENT = "2026-2027-1";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] DEMO_MA = {"841109", "841215", "841401", "841501"};

    private final Student sinhVien = new Student("Nguyá»…n Tháº£o Minh Anh", "22120045",
            "CNTT-K67", "minh.anh@sv.univ.edu.vn");
    private final List<Semester> semesters = new ArrayList<>();
    private final List<Course> courses = new ArrayList<>();
    private final Map<String, List<SectionSlot>> slotsBySemester = new LinkedHashMap<>();
    private final List<StoredPlan> plans = new ArrayList<>();
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();
    private int nextSectionId = 1;
    private long nextPlanId = 1;

    public MockNetworkClient() {
        seedSemesters();
        seedCourses();
        seedSlots();
        seedPlans();
    }

    // ------------------------------------------------------------------ seed

    private void seedSemesters() {
        semesters.add(new Semester("2025-2026-1", "Há»c ká»³ I - NÄƒm há»c 2025-2026", false,
                "01/09/2025", "14/12/2025"));
        semesters.add(new Semester("2025-2026-2", "Há»c ká»³ II - NÄƒm há»c 2025-2026", false,
                "02/03/2026", "24/05/2026"));
        semesters.add(new Semester(CURRENT, "Há»c ká»³ I - NÄƒm há»c 2026-2027", true,
                "14/09/2026", "20/12/2026"));
    }

    private void seedCourses() {
        c("841109", "Ká»¹ thuáº­t láº­p trÃ¬nh", 3);
        c("841215", "Cáº¥u trÃºc dá»¯ liá»‡u vÃ  giáº£i thuáº­t", 3);
        c("841401", "TrÃ­ tuá»‡ nhÃ¢n táº¡o", 3);
        c("841501", "CÃ´ng nghá»‡ pháº§n má»m", 3);
        c("841216", "CÆ¡ sá»Ÿ dá»¯ liá»‡u", 3);
        c("841301", "Máº¡ng mÃ¡y tÃ­nh", 3);
    }

    private void c(String ma, String ten, int tc) {
        courses.add(new Course(ma, ten, tc));
    }

    private String tenCua(String maMh) {
        for (Course c : courses) {
            if (c.maMh().equals(maMh)) {
                return c.ten();
            }
        }
        return maMh;
    }

    private int tcCua(String maMh) {
        for (Course c : courses) {
            if (c.maMh().equals(maMh)) {
                return c.soTc();
            }
        }
        return 0;
    }

    /**
     * Lich mau (cac nhom duoc bien so de dung ra A/B/c):
     * - 841109: LT01+TH01.1 T2 t1-4 (sang, liá»n máº¡ch); LT02+TH02.1 T2 t1-4.
     * - 841215: LT01+TH01.1 T3 t1-4; LT02+TH02.1 T3 t1-4.
     * - 841401: LT01 T4 t2-3 (sang); LT02 T3 t9-10 (chieu).
     * - 841501: LT01 T4 t4-5 (sang); LT02 T3 t11-12 (chieu); LT03 T4 t6-7 (chieu).
     * A = to hop 01 (3 ngay, 0 trong, 100% sang);
     * B = to hop 02 (2 ngay, 4 trong = 240 phut, 8/12 sang = 67%);
     * C = 01 + 841401 LT01 + 841501 LT03 (3 ngay, 2 trong = 120 phut, 10/12 = 83%).
     */
    private void seedSlots() {
        for (Semester s : semesters) {
            String k = s.ma();
            List<SectionSlot> l = new ArrayList<>();
            l.add(nhom(k, "841109", "01", "", "LT", 2, 1, 2, "P.201", "TS. Tráº§n Minh Äá»©c", 45));
            l.add(nhom(k, "841109", "01", "1", "TH", 2, 3, 2, "P.304", "ThS. LÃª Thu HÃ ", 32));
            l.add(nhom(k, "841109", "02", "", "LT", 2, 1, 2, "P.202", "TS. Tráº§n Minh Äá»©c", 44));
            l.add(nhom(k, "841109", "02", "1", "TH", 2, 3, 2, "P.305", "ThS. LÃª Thu HÃ ", 31));
            l.add(nhom(k, "841215", "01", "", "LT", 3, 1, 2, "P.205", "PGS. Nguyá»…n VÄƒn An", 50));
            l.add(nhom(k, "841215", "01", "1", "TH", 3, 3, 2, "P.306", "ThS. Pháº¡m Quang Huy", 30));
            l.add(nhom(k, "841215", "02", "", "LT", 3, 1, 2, "P.206", "PGS. Nguyá»…n VÄƒn An", 48));
            l.add(nhom(k, "841215", "02", "1", "TH", 3, 3, 2, "P.307", "ThS. Pháº¡m Quang Huy", 29));
            l.add(nhom(k, "841401", "01", "", "LT", 4, 2, 2, "P.207", "TS. Äá»— Minh QuÃ¢n", 40));
            l.add(nhom(k, "841401", "02", "", "LT", 3, 9, 2, "P.208", "TS. Äá»— Minh QuÃ¢n", 38));
            l.add(nhom(k, "841501", "01", "", "LT", 4, 4, 2, "P.209", "ThS. VÅ© Thanh TÃ¢m", 42));
            l.add(nhom(k, "841501", "02", "", "LT", 3, 11, 2, "P.210", "ThS. VÅ© Thanh TÃ¢m", 41));
            l.add(nhom(k, "841501", "03", "", "LT", 4, 6, 2, "P.211", "ThS. VÅ© Thanh TÃ¢m", 43));
            l.add(nhom(k, "841216", "01", "", "LT", 5, 6, 2, "P.212", "TS. HoÃ ng Thá»‹ Mai", 39));
            l.add(nhom(k, "841216", "01", "1", "TH", 6, 1, 2, "P.308", "ThS. Nguyá»…n Tháº¿ Anh", 30));
            l.add(nhom(k, "841216", "02", "", "LT", 6, 6, 2, "P.213", "TS. HoÃ ng Thá»‹ Mai", 37));
            l.add(nhom(k, "841216", "02", "1", "TH", 6, 3, 2, "P.309", "ThS. Nguyá»…n Tháº¿ Anh", 28));
            l.add(nhom(k, "841301", "01", "", "LT", 2, 6, 2, "P.214", "PGS.TS LÃ½ Tuáº¥n Kiá»‡t", 46));
            l.add(nhom(k, "841301", "01", "1", "TH", 5, 3, 2, "P.310", "ThS. Trá»‹nh BÃ­ch Ngá»c", 33));
            slotsBySemester.put(k, l);
        }
    }

    private SectionSlot nhom(String maHocKy, String maMh, String nhom, String toTh, String loai,
                             int thu, int tietBd, int soTiet, String phong, String gv, int siSo) {
        Section s = new Section(nextSectionId++, maHocKy, maMh, nhom, toTh, loai, siSo, gv);
        return new SectionSlot(s, tenCua(maMh), tcCua(maMh),
                List.of(new ClassSession(thu, tietBd, soTiet, phong)));
    }

    private void seedPlans() {
        luuCungBo(CURRENT, "Lá»‹ch há»c táº­p trung", 89.0, "16/10/2026 20:15",
                List.of(idOf(CURRENT, "841109", "01", ""), idOf(CURRENT, "841109", "01", "1"),
                        idOf(CURRENT, "841215", "01", ""), idOf(CURRENT, "841215", "01", "1"),
                        idOf(CURRENT, "841401", "01", ""), idOf(CURRENT, "841501", "01", "")));
        luuCungBo(CURRENT, "Lá»‹ch 2 ngÃ y Ã­t nghá»‰", 80.0, "17/10/2026 08:40",
                List.of(idOf(CURRENT, "841109", "02", ""), idOf(CURRENT, "841109", "02", "1"),
                        idOf(CURRENT, "841215", "02", ""), idOf(CURRENT, "841215", "02", "1"),
                        idOf(CURRENT, "841401", "02", ""), idOf(CURRENT, "841501", "02", "")));
    }

    private void luuCungBo(String maHocKy, String ten, double diem, String ngayTao,
                           List<Integer> sectionIds) {
        List<SectionSlot> slots = timSlots(maHocKy, sectionIds);
        StoredPlan p = new StoredPlan();
        p.id = nextPlanId++;
        p.maHocKy = maHocKy;
        p.ten = ten;
        p.diem = diem;
        p.stats = tinhChiSo(slots);
        p.sectionIds = new ArrayList<>(sectionIds);
        p.slots = slots;
        p.ngayTao = ngayTao;
        plans.add(p);
    }

    private int idOf(String maHocKy, String maMh, String nhom, String toTh) {
        for (SectionSlot s : slotsBySemester.getOrDefault(maHocKy, List.of())) {
            Section x = s.section();
            if (x.maMh().equals(maMh) && x.nhom().equals(nhom) && x.toTh().equals(toTh)) {
                return x.id();
            }
        }
        throw new IllegalStateException("Seed thieu nhom " + maMh + "." + nhom + "." + toTh);
    }

    private List<SectionSlot> timSlots(String maHocKy, List<Integer> ids) {
        List<SectionSlot> out = new ArrayList<>();
        for (Integer id : ids) {
            SectionSlot s = timSlot(id);
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ API

    @Override
    public Student sinhVien() {
        return sinhVien;
    }

    @Override
    public List<Semester> listSemesters() {
        return List.copyOf(semesters);
    }

    @Override
    public List<Course> listCourses(String maHocKy) {
        List<String> mo = new ArrayList<>();
        for (SectionSlot slot : slotsBySemester.getOrDefault(maHocKy, List.of())) {
            if (!mo.contains(slot.section().maMh())) {
                mo.add(slot.section().maMh());
            }
        }
        List<Course> result = new ArrayList<>();
        for (Course course : courses) {
            if (mo.contains(course.maMh())) {
                result.add(course);
            }
        }
        return result;
    }

    @Override
    public List<SectionSlot> listSections(String maHocKy, String maMh) {
        List<SectionSlot> result = new ArrayList<>();
        for (SectionSlot slot : slotsBySemester.getOrDefault(maHocKy, List.of())) {
            if (slot.section().maMh().equals(maMh)) {
                result.add(slot);
            }
        }
        return result;
    }

    @Override
    public List<SectionSlot> listAllSections(String maHocKy) {
        return List.copyOf(slotsBySemester.getOrDefault(maHocKy, List.of()));
    }

    @Override
    public String submit(PlanRequest request) throws ApiException {
        List<SectionSlot> tatCa = slotsBySemester.get(request.maHocKy());
        if (tatCa == null) {
            throw new ApiException(ErrorCode.SEMESTER_NOT_FOUND, "Há»c ká»³ khÃ´ng tá»“n táº¡i");
        }
        List<Integer> ids = request.sectionIds() == null ? List.of() : request.sectionIds();
        List<SectionSlot> candidates = new ArrayList<>();
        List<String> monUuTien = request.maMhTheoUuTien() == null
                ? new ArrayList<>() : new ArrayList<>(request.maMhTheoUuTien());
        if (!ids.isEmpty()) {
            for (Integer id : ids) {
                SectionSlot s = timSlot(id);
                if (s == null || !s.section().maHocKy().equals(request.maHocKy())) {
                    throw new ApiException(ErrorCode.VALIDATION_ERROR,
                            "NhÃ³m lá»›p khÃ´ng thuá»™c há»c ká»³ Ä‘Ã£ chá»n");
                }
                candidates.add(s);
                String ma = s.section().maMh();
                if (!monUuTien.contains(ma)) {
                    monUuTien.add(ma);
                }
            }
        } else {
            if (monUuTien.isEmpty()) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "ChÆ°a chá»n nhÃ³m lá»›p há»c pháº§n nÃ o");
            }
            for (SectionSlot s : tatCa) {
                if (monUuTien.contains(s.section().maMh())) {
                    candidates.add(s);
                }
            }
        }
        List<String> khongMo = new ArrayList<>();
        for (String ma : monUuTien) {
            boolean mo = false;
            for (SectionSlot s : tatCa) {
                if (s.section().maMh().equals(ma)) {
                    mo = true;
                    break;
                }
            }
            if (!mo) {
                khongMo.add(ma);
            }
        }
        if (!khongMo.isEmpty()) {
            throw new ApiException(ErrorCode.COURSE_NOT_OPEN,
                    "CÃ³ há»c pháº§n khÃ´ng má»Ÿ trong há»c ká»³ nÃ y", khongMo);
        }
        kiemTraPhoiHop(tatCa, candidates);

        Criteria criteria = request.criteria() == null ? Criteria.macDinh() : request.criteria();
        PlanRequest chinh = new PlanRequest(request.maHocKy(), monUuTien, criteria, ids);
        String jobId = "job-" + System.nanoTime();
        JobState state = new JobState(jobId, chinh, candidates);
        jobs.put(jobId, state);
        Thread worker = new Thread(() -> runJob(state), "mock-job-" + jobId);
        worker.setDaemon(true);
        state.worker = worker;
        worker.start();
        return jobId;
    }

    /**
     * Kiem tra tinh phoi hop LT/TH: TH phai kem LT cung nhom; LT cua hoc phan co TH
     * phai kem TH cung nhom (LT01 <-> TH01.1, LT02 <-> TH02.1).
     */
    private void kiemTraPhoiHop(List<SectionSlot> tatCa, List<SectionSlot> candidates)
            throws ApiException {
        Set<Integer> daChon = new HashSet<>();
        for (SectionSlot s : candidates) {
            daChon.add(s.section().id());
        }
        for (SectionSlot s : candidates) {
            Section x = s.section();
            if ("TH".equals(x.loai())) {
                boolean coLt = false;
                for (SectionSlot k : tatCa) {
                    Section y = k.section();
                    if ("LT".equals(y.loai()) && y.maMh().equals(x.maMh())
                            && y.nhom().equals(x.nhom()) && daChon.contains(y.id())) {
                        coLt = true;
                        break;
                    }
                }
                if (!coLt) {
                    throw new ApiException(ErrorCode.VALIDATION_ERROR,
                            "NhÃ³m thá»±c hÃ nh " + tenTh(x) + " pháº£i chá»n cÃ¹ng lÃ½ thuyáº¿t "
                                    + tenLt(x.maMh(), x.nhom()) + ".");
                }
            } else {
                boolean coThGoc = false;
                for (SectionSlot k : tatCa) {
                    Section y = k.section();
                    if ("TH".equals(y.loai()) && y.maMh().equals(x.maMh())
                            && y.nhom().equals(x.nhom())) {
                        coThGoc = true;
                        break;
                    }
                }
                if (coThGoc) {
                    boolean coTh = false;
                    for (SectionSlot k : candidates) {
                        Section y = k.section();
                        if ("TH".equals(y.loai()) && y.maMh().equals(x.maMh())
                                && y.nhom().equals(x.nhom())) {
                            coTh = true;
                            break;
                        }
                    }
                    if (!coTh) {
                        throw new ApiException(ErrorCode.VALIDATION_ERROR,
                                "NhÃ³m lÃ½ thuyáº¿t " + tenLt(x.maMh(), x.nhom())
                                        + " pháº£i chá»n cÃ¹ng nhÃ³m thá»±c hÃ nh tÆ°Æ¡ng á»©ng.");
                    }
                }
            }
        }
    }

    private static String tenLt(String maMh, String nhom) {
        return "LT" + nhom + " (" + maMh + ")";
    }

    private static String tenTh(Section x) {
        return "TH" + x.nhom() + "." + x.toTh() + " (" + x.maMh() + ")";
    }

    private void runJob(JobState state) {
        try {
            for (int p = 0; p <= 92; p += 4) {
                if (state.cancelled) {
                    state.state = JobStatus.State.CANCELLED;
                    return;
                }
                state.progress = p;
                Thread.sleep(110);
            }
            JobResult result = tinhKetQua(state.request, state.candidates, state.seed);
            if (state.cancelled) {
                state.state = JobStatus.State.CANCELLED;
                return;
            }
            state.result = result;
            state.progress = 100;
            state.state = JobStatus.State.DONE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            state.state = JobStatus.State.CANCELLED;
        } catch (RuntimeException e) {
            state.error = e.getMessage();
            state.state = JobStatus.State.FAILED;
        }
    }

    @Override
    public JobStatus jobStatus(String jobId) throws ApiException {
        JobState state = jobs.get(jobId);
        if (state == null) {
            throw new ApiException(ErrorCode.JOB_NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y cÃ´ng viá»‡c " + jobId);
        }
        return new JobStatus(jobId, state.state, state.progress, state.result, state.error);
    }

    @Override
    public void cancel(String jobId) throws ApiException {
        JobState state = jobs.get(jobId);
        if (state == null) {
            throw new ApiException(ErrorCode.JOB_NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y cÃ´ng viá»‡c " + jobId);
        }
        state.cancelled = true;
    }

    // -------------------------------------------------------------- thuat toan

    private JobResult tinhKetQua(PlanRequest request, List<SectionSlot> candidates, long jobSeed) {
        Map<String, List<SectionSlot>> theoMon = new LinkedHashMap<>();
        for (String ma : request.maMhTheoUuTien()) {
            theoMon.put(ma, new ArrayList<>());
        }
        for (SectionSlot s : candidates) {
            theoMon.computeIfAbsent(s.section().maMh(), k -> new ArrayList<>()).add(s);
        }
        Map<String, List<List<SectionSlot>>> toHop = new LinkedHashMap<>();
        for (Map.Entry<String, List<SectionSlot>> e : theoMon.entrySet()) {
            toHop.put(e.getKey(), cacToHop(e.getValue(), request.criteria().ngayChon()));
        }

        List<ScheduleOption> options = new ArrayList<>();
        boolean demo = laDemo(request, candidates);
        if (demo) {
            for (ScheduleOption crafted : phuongAnDemo(request, candidates)) {
                options.add(crafted);
            }
        }
        if (options.isEmpty()) {
            int n = Math.max(1, Math.min(request.criteria().soPhuongAn(), 10));
            Set<String> daCo = new HashSet<>();
            for (int i = 0; i < n * 8 && options.size() < n; i++) {
                ScheduleOption o = sinhPhuongAn(request, toHop, jobSeed + i * 977);
                if (!o.slots().isEmpty() && daCo.add(chuaKhoa(o.slots()))) {
                    options.add(o);
                }
            }
        }
        options.sort(Comparator.comparingDouble(ScheduleOption::diem).reversed());
        List<ScheduleOption> ranked = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            ScheduleOption o = options.get(i);
            String ten = String.valueOf((char) ('A' + Math.min(i, 25)));
            ranked.add(new ScheduleOption(i + 1, ten, o.diem(), o.stats(), o.moTa(), o.slots(),
                    o.biLoai(), o.nhomThayThe()));
        }
        return new JobResult(request.maHocKy(), ranked);
    }

    /** Nhom hop le: neu hoc phan co TH thi chi cac cap LT+TH cung nhom; khac thi tung LT rieng. */
    private List<List<SectionSlot>> cacToHop(List<SectionSlot> ungVien, List<Integer> ngayChon) {
        List<List<SectionSlot>> out = new ArrayList<>();
        List<SectionSlot> lts = new ArrayList<>();
        List<SectionSlot> ths = new ArrayList<>();
        for (SectionSlot s : ungVien) {
            if ("TH".equals(s.section().loai())) {
                ths.add(s);
            } else {
                lts.add(s);
            }
        }
        Set<Integer> ngayHopLe = ngayChon == null || ngayChon.isEmpty()
                ? null : new HashSet<>(ngayChon);
        if (ths.isEmpty()) {
            for (SectionSlot lt : lts) {
                if (phuHopNgay(List.of(lt), ngayHopLe)) {
                    out.add(List.of(lt));
                }
            }
            return out;
        }
        List<String> nhoms = new ArrayList<>();
        for (SectionSlot lt : lts) {
            if (!nhoms.contains(lt.section().nhom())) {
                nhoms.add(lt.section().nhom());
            }
        }
        for (String nhom : nhoms) {
            SectionSlot lt = null;
            for (SectionSlot s : lts) {
                if (s.section().nhom().equals(nhom)) {
                    lt = s;
                    break;
                }
            }
            List<SectionSlot> th = new ArrayList<>();
            for (SectionSlot s : ths) {
                if (s.section().nhom().equals(nhom)) {
                    th.add(s);
                }
            }
            if (th.isEmpty()) {
                continue;
            }
            List<SectionSlot> toHop = new ArrayList<>();
            toHop.add(lt);
            toHop.addAll(th);
            if (phuHopNgay(toHop, ngayHopLe)) {
                out.add(toHop);
            }
        }
        return out;
    }

    private boolean phuHopNgay(List<SectionSlot> slots, Set<Integer> ngayHopLe) {
        if (ngayHopLe == null) {
            return true;
        }
        for (SectionSlot s : slots) {
            for (ClassSession cs : s.sessions()) {
                if (!ngayHopLe.contains(cs.thu())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean laDemo(PlanRequest request, List<SectionSlot> candidates) {
        Set<String> mon = new HashSet<>(request.maMhTheoUuTien());
        if (mon.size() != DEMO_MA.length) {
            return false;
        }
        for (String ma : DEMO_MA) {
            if (!mon.contains(ma)) {
                return false;
            }
        }
        Set<Integer> demoIds = demoSectionIds(request.maHocKy());
        for (SectionSlot s : candidates) {
            if (!demoIds.contains(s.section().id())) {
                return false;
            }
        }
        return candidates.size() == demoIds.size();
    }

    private Set<Integer> demoSectionIds(String maHocKy) {
        Set<Integer> ids = new HashSet<>();
        for (SectionSlot s : slotsBySemester.getOrDefault(maHocKy, List.of())) {
            Section x = s.section();
            for (String ma : DEMO_MA) {
                if (!x.maMh().equals(ma)) {
                    continue;
                }
                // Chi tinh cac nhom thuoc to hop 01 (phuong an A) lam bo minh hoa.
                if ("01".equals(x.nhom())) {
                    ids.add(x.id());
                }
            }
        }
        return ids;
    }

    private List<ScheduleOption> phuongAnDemo(PlanRequest request, List<SectionSlot> candidates) {
        Set<Integer> choPhep = new HashSet<>();
        for (SectionSlot s : candidates) {
            choPhep.add(s.section().id());
        }
        String k = request.maHocKy();
        List<List<Integer>> boBa = List.of(
                List.of(idOf(k, "841109", "01", ""), idOf(k, "841109", "01", "1"),
                        idOf(k, "841215", "01", ""), idOf(k, "841215", "01", "1"),
                        idOf(k, "841401", "01", ""), idOf(k, "841501", "01", "")),
                List.of(idOf(k, "841109", "02", ""), idOf(k, "841109", "02", "1"),
                        idOf(k, "841215", "02", ""), idOf(k, "841215", "02", "1"),
                        idOf(k, "841401", "02", ""), idOf(k, "841501", "02", "")),
                List.of(idOf(k, "841109", "01", ""), idOf(k, "841109", "01", "1"),
                        idOf(k, "841215", "01", ""), idOf(k, "841215", "01", "1"),
                        idOf(k, "841401", "01", ""), idOf(k, "841501", "03", "")));
        List<Double> diem = List.of(89.0, 80.0, 78.0);
        List<String> moTa = List.of(
                "ToÃ n bá»™ buá»•i sÃ¡ng, khÃ´ng cÃ³ tiáº¿t trá»‘ng",
                "Ãt ngÃ y Ä‘áº¿n trÆ°á»ng nháº¥t vá»›i 2 ngÃ y há»c",
                "CÃ¢n báº±ng giá»¯a sá»‘ ngÃ y há»c vÃ  khoáº£ng nghá»‰");
        List<ScheduleOption> out = new ArrayList<>();
        for (int i = 0; i < boBa.size(); i++) {
            List<Integer> ids = boBa.get(i);
            boolean dayDu = true;
            for (Integer id : ids) {
                if (!choPhep.contains(id)) {
                    dayDu = false;
                    break;
                }
            }
            // Phuong an A phai nam trong bo da chon; phuong an B va C la cac to hop thay the
            // cung hoc phan (khac nhom) de the hien su lua chon 2 ngay/khong gap.
            if (i >= 1) {
                dayDu = true;
                for (Integer id : ids) {
                    SectionSlot s = timSlot(id);
                    if (s == null) {
                        dayDu = false;
                        break;
                    }
                }
            }
            if (!dayDu) {
                continue;
            }
            List<SectionSlot> slots = timSlots(k, ids);
            if (phuHopNgay(slots, ngayHopLe(request.criteria()))) {
                out.add(new ScheduleOption(0, "", diem.get(i), tinhChiSo(slots), moTa.get(i),
                        slots, List.of(), List.of()));
            }
        }
        return out;
    }

    private Set<Integer> ngayHopLe(Criteria c) {
        return c.ngayChon() == null || c.ngayChon().isEmpty()
                ? null : new HashSet<>(c.ngayChon());
    }

    private ScheduleOption sinhPhuongAn(PlanRequest request,
                                        Map<String, List<List<SectionSlot>>> toHop, long seed) {
        Random r = new Random(seed);
        List<SectionSlot> chon = new ArrayList<>();
        List<String> biLoai = new ArrayList<>();
        List<SectionSlot> thayThe = new ArrayList<>();
        for (String ma : request.maMhTheoUuTien()) {
            List<List<SectionSlot>> u = new ArrayList<>(
                    toHop.getOrDefault(ma, List.of()));
            Collections.shuffle(u, r);
            boolean dat = false;
            for (List<SectionSlot> toHopChon : u) {
                if (khongTrung(chon, toHopChon)) {
                    chon.addAll(toHopChon);
                    dat = true;
                    Set<Integer> chonIds = new HashSet<>();
                    for (SectionSlot s : toHopChon) {
                        chonIds.add(s.section().id());
                    }
                    for (List<SectionSlot> khac : toHop.getOrDefault(ma, List.of())) {
                        for (SectionSlot s : khac) {
                            if (!chonIds.contains(s.section().id())) {
                                thayThe.add(s);
                            }
                        }
                    }
                    break;
                }
            }
            if (!dat) {
                biLoai.add(ma);
            }
        }
        OptionStats stats = tinhChiSo(chon);
        double diem = tinhDiem(request.criteria(), stats, biLoai.size(), chon);
        String moTa = String.format("%d ngÃ y há»c Â· %d phÃºt trá»‘ng Â· %d%% tiáº¿t buá»•i sÃ¡ng",
                stats.soNgayHoc(), stats.phutTrong(), stats.tyLeSang());
        return new ScheduleOption(0, "", diem, stats, moTa, chon, biLoai, thayThe);
    }

    private boolean khongTrung(List<SectionSlot> daChon, List<SectionSlot> them) {
        for (SectionSlot a : them) {
            for (SectionSlot b : daChon) {
                if (a.trung(b)) {
                    return false;
                }
            }
            for (SectionSlot b : them) {
                if (a != b && a.trung(b)) {
                    return false;
                }
            }
        }
        return true;
    }

    private OptionStats tinhChiSo(List<SectionSlot> slots) {
        Map<Integer, boolean[]> theoNgay = new LinkedHashMap<>();
        int tongTiet = 0;
        for (SectionSlot slot : slots) {
            for (ClassSession cs : slot.sessions()) {
                boolean[] tiet = theoNgay.computeIfAbsent(cs.thu(), k -> new boolean[13]);
                for (int t = cs.tietBd(); t < cs.tietBd() + cs.soTiet() && t <= 12; t++) {
                    if (!tiet[t]) {
                        tiet[t] = true;
                        tongTiet++;
                    }
                }
            }
        }
        int soNgay = 0;
        int soTietTrong = 0;
        int soTietSang = 0;
        for (boolean[] tiet : theoNgay.values()) {
            soNgay++;
            int dau = -1;
            int cuoi = -1;
            for (int t = 1; t <= 12; t++) {
                if (tiet[t]) {
                    if (dau == -1) {
                        dau = t;
                    }
                    cuoi = t;
                    if (t <= 5) {
                        soTietSang++;
                    }
                }
            }
            for (int t = dau; t <= cuoi; t++) {
                if (!tiet[t]) {
                    soTietTrong++;
                }
            }
        }
        return new OptionStats(soNgay, soTietTrong, soTietSang, tongTiet);
    }

    private double tinhDiem(Criteria c, OptionStats stats, int soBiLoai, List<SectionSlot> slots) {
        double diem = 100;
        diem -= stats.soNgayHoc() * 2.0 * c.itNgay() / 5.0;
        diem -= stats.soTietTrong() * 2.0 * c.khoangTrong() / 5.0;
        diem -= (100 - stats.tyLeSang()) / 100.0 * 30.0 * c.uuTienSang() / 5.0;
        if (c.ngayChon() != null && !c.ngayChon().isEmpty() && c.ngayYeuThich() > 0) {
            Set<Integer> chon = new HashSet<>(c.ngayChon());
            int ngoai = 0;
            Set<Integer> daDi = new HashSet<>();
            for (SectionSlot s : slots) {
                for (ClassSession cs : s.sessions()) {
                    if (!chon.contains(cs.thu())) {
                        daDi.add(cs.thu());
                    }
                }
            }
            ngoai = daDi.size();
            diem -= ngoai * 5.0 * c.ngayYeuThich() / 5.0;
        }
        diem -= soBiLoai * 20.0;
        return Math.max(0, Math.min(100, Math.round(diem * 100.0) / 100.0));
    }

    private String chuaKhoa(List<SectionSlot> slots) {
        List<Integer> ids = new ArrayList<>();
        for (SectionSlot s : slots) {
            ids.add(s.section().id());
        }
        Collections.sort(ids);
        return ids.toString();
    }

    // ------------------------------------------------------------- phuong an

    @Override
    public List<SavedPlan> listPlans(String maHocKy) {
        List<SavedPlan> result = new ArrayList<>();
        for (StoredPlan p : plans) {
            if (maHocKy == null || maHocKy.equals(p.maHocKy)) {
                int[] soLieu = tinhSoLieu(p.slots);
                result.add(new SavedPlan(p.id, p.maHocKy, p.ten, p.diem, p.stats, p.ngayTao,
                        soLieu[0], soLieu[1], soLieu[2]));
            }
        }
        result.sort(Comparator.comparing(SavedPlan::ngayTao).reversed());
        return result;
    }

    private int[] tinhSoLieu(List<SectionSlot> slots) {
        Set<String> mon = new HashSet<>();
        int tc = 0;
        int buoi = 0;
        for (SectionSlot s : slots) {
            if (mon.add(s.section().maMh())) {
                tc += s.soTc();
            }
            buoi += s.sessions().size();
        }
        return new int[]{mon.size(), tc, buoi};
    }

    @Override
    public PlanDetail getPlan(long planId) throws ApiException {
        StoredPlan p = findPlan(planId);
        return new PlanDetail(p.id, p.maHocKy, p.ten, p.diem, p.stats, p.slots, p.biLoai,
                p.ngayTao);
    }

    @Override
    public long savePlan(String maHocKy, String ten, List<Integer> sectionIds) throws ApiException {
        for (StoredPlan p : plans) {
            if (p.ten.equalsIgnoreCase(ten)) {
                throw new ApiException(ErrorCode.NAME_TAKEN,
                        "TÃªn phÆ°Æ¡ng Ã¡n \"" + ten + "\" Ä‘Ã£ Ä‘Æ°á»£c dÃ¹ng");
            }
        }
        List<SectionSlot> slots = timSlots(maHocKy, sectionIds);
        OptionStats stats = tinhChiSo(slots);
        double diem = tinhDiem(Criteria.macDinh(), stats, 0, slots);
        StoredPlan plan = new StoredPlan();
        plan.id = nextPlanId++;
        plan.maHocKy = maHocKy;
        plan.ten = ten;
        plan.diem = diem;
        plan.stats = stats;
        plan.slots = slots;
        plan.sectionIds = new ArrayList<>(sectionIds);
        plan.ngayTao = LocalDateTime.now().format(FMT);
        plans.add(plan);
        return plan.id;
    }

    @Override
    public void deletePlan(long planId) throws ApiException {
        StoredPlan p = findPlan(planId);
        plans.remove(p);
    }

    @Override
    public void renamePlan(long planId, String tenMoi) throws ApiException {
        StoredPlan p = findPlan(planId);
        String ten = tenMoi == null ? "" : tenMoi.trim();
        if (ten.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "TÃªn phÆ°Æ¡ng Ã¡n khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng");
        }
        for (StoredPlan khac : plans) {
            if (khac.id != planId && khac.ten.equalsIgnoreCase(ten)) {
                throw new ApiException(ErrorCode.NAME_TAKEN,
                        "TÃªn phÆ°Æ¡ng Ã¡n \"" + ten + "\" Ä‘Ã£ Ä‘Æ°á»£c dÃ¹ng");
            }
        }
        p.ten = ten;
    }

    private StoredPlan findPlan(long id) throws ApiException {
        for (StoredPlan p : plans) {
            if (p.id == id) {
                return p;
            }
        }
        throw new ApiException(ErrorCode.PLAN_NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y phÆ°Æ¡ng Ã¡n Ä‘Ã£ lÆ°u");
    }

    private SectionSlot timSlot(int id) {
        for (List<SectionSlot> list : slotsBySemester.values()) {
            for (SectionSlot slot : list) {
                if (slot.section().id() == id) {
                    return slot;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ state

    private static final class StoredPlan {
        long id;
        String maHocKy;
        String ten;
        double diem;
        OptionStats stats;
        List<Integer> sectionIds;
        List<SectionSlot> slots;
        List<String> biLoai = new ArrayList<>();
        String ngayTao;
    }

    private static final class JobState {
        final String id;
        final PlanRequest request;
        final List<SectionSlot> candidates;
        final long seed = System.nanoTime();
        volatile int progress;
        volatile boolean cancelled;
        volatile JobStatus.State state = JobStatus.State.RUNNING;
        volatile JobResult result;
        volatile String error;
        Thread worker;

        JobState(String id, PlanRequest request, List<SectionSlot> candidates) {
            this.id = id;
            this.request = request;
            this.candidates = candidates;
        }
    }
}
