package com.tkb.backend.service;

import com.tkb.backend.dao.CatalogDao;
import com.tkb.common.dto.SectionDto;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * B5.1 - CatalogCache: Cache danh sach nhom lop (SectionDto kem SlotDto) trong RAM theo tung hoc ky.
 *
 * Muc dich:
 *   - Giup thuat toan xep lich OPTIMIZE (B6) va cac lenh GET_SECTIONS, SAVE_PLAN truy xuat
 *     nhom lop truc tiep tu RAM ma KHONG truy van MySQL trong vong lap.
 *   - Tu dong lam moi (invalidate / reload) ngay sau khi Admin import PDF hoac them/sua/xoa/tat nhom lop.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/service/CatalogCache.java
 */
public class CatalogCache {

    private final CatalogDao catalogDao;

    /** Cache theo ma hoc ky -> Du lieu nhom lop da phan loai trong hoc ky do */
    private final Map<String, SemesterSnapshot> semesterCache = new ConcurrentHashMap<>();

    public CatalogCache(CatalogDao catalogDao) {
        this.catalogDao = catalogDao;
    }

    /**
     * Lay danh sach cac nhom lop DANG HOAT DONG (active = true) cua 1 hoc phan trong hoc ky tu RAM Cache.
     */
    public List<SectionDto> getActiveSectionsByCourse(String semesterCode, String courseCode) throws SQLException {
        SemesterSnapshot snapshot = getOrLoadSnapshot(semesterCode);
        return snapshot.activeByCourse.getOrDefault(courseCode, Collections.emptyList());
    }

    /**
     * Lay Map (courseCode -> List<SectionDto> dang hoat dong) cua toan bo hoc ky tu RAM Cache.
     */
    public Map<String, List<SectionDto>> getAllActiveSectionsByCourse(String semesterCode) throws SQLException {
        SemesterSnapshot snapshot = getOrLoadSnapshot(semesterCode);
        return snapshot.activeByCourse;
    }

    /**
     * Lay nhom lop theo ID trong hoc ky (ho tro SAVE_PLAN kiem tra nhanh cac sectionIds ma Client gui len).
     */
    public SectionDto getSectionById(String semesterCode, long sectionId) throws SQLException {
        SemesterSnapshot snapshot = getOrLoadSnapshot(semesterCode);
        return snapshot.bySectionId.get(sectionId);
    }

    /**
     * Lay danh sach nhom lop cua 1 hoc phan (phuc vu GET_SECTIONS).
     * Neu includeInactive = false -> doc thang tu RAM Cache.
     * Neu includeInactive = true (Admin) -> lay ca nhom dang tat.
     */
    public List<SectionDto> getSectionsForCourse(String semesterCode, String courseCode, boolean includeInactive) throws SQLException {
        SemesterSnapshot snapshot = getOrLoadSnapshot(semesterCode);
        if (!includeInactive) {
            return snapshot.activeByCourse.getOrDefault(courseCode, Collections.emptyList());
        }
        return snapshot.allByCourse.getOrDefault(courseCode, Collections.emptyList());
    }

    /**
     * Xoa va nap lai cache cua 1 hoc ky sau khi Admin sua du lieu hoac Import PDF xong (B5.1, B9.4, B9.7).
     */
    public void refreshSemester(String semesterCode) {
        if (semesterCode == null || semesterCode.isBlank()) {
            return;
        }
        semesterCache.remove(semesterCode.trim());
    }

    /**
     * Xoa toan bo cache cua moi hoc ky.
     */
    public void clearAll() {
        semesterCache.clear();
    }

    private SemesterSnapshot getOrLoadSnapshot(String semesterCode) throws SQLException {
        String key = semesterCode.trim();
        SemesterSnapshot existing = semesterCache.get(key);
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = semesterCache.get(key);
            if (existing != null) {
                return existing;
            }
            List<SectionDto> allSections = catalogDao.loadSectionsBySemester(key, true);
            SemesterSnapshot loaded = new SemesterSnapshot(allSections);
            semesterCache.put(key, loaded);
            return loaded;
        }
    }

    /**
     * Snapshot bat bien (immutable) luu trang thai nhom lop cua 1 hoc ky trong RAM.
     */
    private static final class SemesterSnapshot {
        final Map<Long, SectionDto> bySectionId;
        final Map<String, List<SectionDto>> activeByCourse;
        final Map<String, List<SectionDto>> allByCourse;

        SemesterSnapshot(List<SectionDto> sections) {
            Map<Long, SectionDto> idMap = new LinkedHashMap<>();
            Map<String, List<SectionDto>> activeMap = new LinkedHashMap<>();
            Map<String, List<SectionDto>> allMap = new LinkedHashMap<>();

            for (SectionDto sec : sections) {
                if (sec.getId() != null) {
                    idMap.put(sec.getId(), sec);
                }
                String courseCode = sec.getCourseCode();
                allMap.computeIfAbsent(courseCode, k -> new ArrayList<>()).add(sec);
                if (sec.isActive()) {
                    activeMap.computeIfAbsent(courseCode, k -> new ArrayList<>()).add(sec);
                }
            }

            this.bySectionId = Collections.unmodifiableMap(idMap);
            this.activeByCourse = Collections.unmodifiableMap(activeMap);
            this.allByCourse = Collections.unmodifiableMap(allMap);
        }
    }
}