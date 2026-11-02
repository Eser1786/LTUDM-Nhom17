package com.tkb.common.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B2.3 - Request: Doi tuong dai dien cho goi tin yeu cau tu Client gui len Server (Muc 4 Protocol).
 *
 * Cau truc JSON chuan:
 *   {"id": 12, "cmd": "LOGIN", "data": {"username": "eser", "password": "..."}}
 *
 * Luu y:
 *   - id: So nguyen duong do Client tu tang dan tu 1 tren moi ket noi.
 *   - cmd: Ten lenh (khop voi enum Command).
 *   - data: Map chua tham so dau vao (mac dinh la "{}" neu lenh khong co tham so).
 *   - Tich hop san cac ham doc tham so an toan (getString, getInt, getBoolean, getStringList)
 *     tuong thich voi ca Gson va Jackson (tranh loi ep kieu Double/Integer).
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/protocol/Request.java
 */
public class Request {

    private Integer id;
    private String cmd;
    private Map<String, Object> data;

    public Request() {
        this.data = new LinkedHashMap<>();
    }

    public Request(Integer id, String cmd, Map<String, Object> data) {
        this.id = id;
        this.cmd = cmd;
        this.data = (data != null) ? data : new LinkedHashMap<>();
    }

    public Request(Integer id, Command command, Map<String, Object> data) {
        this(id, command != null ? command.name() : null, data);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    /**
     * Chuyen chuoi cmd sang enum Command (tra ve null neu lenh khong hop le).
     */
    public Command resolveCommand() {
        return Command.fromString(cmd);
    }

    public Map<String, Object> getData() {
        if (data == null) {
            data = new LinkedHashMap<>();
        }
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = (data != null) ? data : new LinkedHashMap<>();
    }

    // =========================================================================
    // CAC HAM TIEN ICH LAY THAM SO AN TOAN TU "data" (HO TRO CA GSON & JACKSON)
    // =========================================================================

    public Request put(String key, Object value) {
        getData().put(key, value);
        return this;
    }

    public String getString(String key) {
        if (data == null) return null;
        Object val = data.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    public Integer getInt(String key) {
        if (data == null) return null;
        Object val = data.get(key);
        if (val instanceof Number number) {
            return number.intValue();
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public Long getLong(String key) {
        if (data == null) return null;
        Object val = data.get(key);
        if (val instanceof Number number) {
            return number.longValue();
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public Boolean getBoolean(String key) {
        if (data == null) return null;
        Object val = data.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return null;
    }

    public List<String> getStringList(String key) {
        if (data == null) return Collections.emptyList();
        Object val = data.get(key);
        if (val instanceof List<?> rawList) {
            List<String> result = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    public List<Integer> getIntList(String key) {
        if (data == null) return Collections.emptyList();
        Object val = data.get(key);
        if (val instanceof List<?> rawList) {
            List<Integer> result = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (item instanceof Number n) {
                    result.add(n.intValue());
                } else if (item != null) {
                    result.add(Integer.parseInt(String.valueOf(item).trim()));
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}