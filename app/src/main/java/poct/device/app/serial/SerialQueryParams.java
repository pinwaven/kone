package poct.device.app.serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import info.szyh.common4.codec.EncodeUtils;
import info.szyh.common4.lang.StringUtils;

public class SerialQueryParams {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    public static final SerialQueryParams EMPTY = new SerialQueryParams();

    /**
     * 参数分隔符
     */
    private String paramSep = "&";
    private final List<KVP> query = new ArrayList<>();
    private boolean urlEncoded = true;

    public SerialQueryParams(boolean urlEncoded) {
        this(null, urlEncoded, "&");
    }

    public SerialQueryParams(String queryString) {
        this(queryString, true, "&");
    }

    public SerialQueryParams(String queryString, boolean urlEncoded) {
        this(queryString, urlEncoded, "&");
    }

    public SerialQueryParams(String queryString, boolean urlEncoded, String sep) {
        this.urlEncoded = urlEncoded;
        this.paramSep = sep;
        if (StringUtils.isNotEmpty(queryString)) {
            parse(queryString);
        }
    }

    public SerialQueryParams() {
    }

    public boolean contains(String key) {
        for (KVP kvp : query) {
            if (kvp.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public SerialQueryParams addParam(String key, String value) {
        if (key == null || value == null) {
//            throw new NullPointerException("null parameter key or value");
            if (logger.isDebugEnabled()) {
                logger.debug("null parameter key or value");
            }
        }
        query.add(new KVP(key, value));
        return this;
    }

    /**
     * 移除该key的所有值
     */
    public SerialQueryParams removeParams(String key) {
        if (key == null) {
            return this;
        }
        List<KVP> forRemove = new ArrayList<>();
        for (KVP kvp : query) {
            if (kvp.key.equals(key)) {
                forRemove.add(kvp);
            }
        }
        for (KVP kvp : forRemove) {
            query.remove(kvp);
        }
        return this;
    }

    private void parse(String queryString) {
        String[] split = queryString.split(this.paramSep);
        for (String pair : split) {
            int eqIndex = pair.indexOf("=");
            if (urlEncoded) {
                if (eqIndex < 0) {
                    // key with no value
                    addParam(EncodeUtils.decodeUrl(pair), "");
                } else {
                    // key=value
                    String key = EncodeUtils.decodeUrl(pair.substring(0, eqIndex));
                    String value = EncodeUtils.decodeUrl(pair.substring(eqIndex + 1));
                    query.add(new KVP(key, value));
                }
            } else {
                if (eqIndex < 0) {
                    // key with no value
                    addParam(pair, "");
                } else {
                    // key=value
                    String key = pair.substring(0, eqIndex);
                    String value = pair.substring(eqIndex + 1);
                    query.add(new KVP(key, value));
                }
            }

        }
    }

    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        for (KVP kvp : query) {
            if (sb.length() > 0) {
                sb.append(paramSep);
            }
            sb.append(urlEncoded ? EncodeUtils.encodeUrl(kvp.key) : kvp.key);
            if (StringUtils.isNotEmpty(kvp.value)) {
                sb.append('=');
                sb.append(urlEncoded ? EncodeUtils.encodeUrl(kvp.value) : kvp.value);
            }
        }
        return sb.toString();
    }

    public String getParameter(String key) {
        for (KVP kvp : query) {
            if (kvp.key.equals(key)) {
                return kvp.value;
            }
        }
        return null;
    }

    public List<String> getParameterValues(String key) {
        List<String> list = new LinkedList<>();
        for (KVP kvp : query) {
            if (kvp.key.equals(key)) {
                list.add(kvp.value);
            }
        }
        return list;
    }

    private static class KVP {
        final String key;
        final String value;

        KVP(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
