package org.xresloader.core.data.dst;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2016/04/07.
 */
public class DataDstJavascript extends DataDstJava {
    private String endl = "\n";
    private String ident = "    ";

    private enum EXPORT_MODE {
        GLOBAL, NODEJS, AMD
    }

    @Override
    public boolean init() {
        if (ProgramOptions.getInstance().prettyIndent <= 0) {
            endl = " ";
            ident = "";
        } else {
            endl = getSystemEndl();

            ident = "";
            for (int i = 0; i < ProgramOptions.getInstance().prettyIndent; ++i) {
                ident += " ";
            }
        }
        return true;
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "javascript";
    }

    @Override
    public final byte[] build(DataDstImpl compiler) throws ConvException {
        DataDstObject data_obj = build_data(compiler);
        StringBuffer sb = new StringBuffer();

        HashMap<String, Object> conv_type = new HashMap<String, Object>();
        for (Map.Entry<String, List<Object>> data_item : data_obj.body.entrySet()) {
            conv_type.put(data_item.getKey(), data_item.getValue());
        }

        for (Map.Entry<String, List<Object>> data_item : data_obj.body.entrySet()) {
            String header_name = String.format("%s_header", data_item.getKey());
            String data_message_type = String.format("%s_data_message_type", data_item.getKey());
            if (!conv_type.containsKey(data_message_type)) {
                conv_type.put(data_message_type, data_obj.data_message_type);
            }
            if (!conv_type.containsKey(header_name)) {
                conv_type.put(header_name, data_obj.header);
                break;
            }
        }

        writeExport(sb, conv_type, 0);

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    }

    @Override
    public final DataDstWriterNode compile() {
        this.logErrorMessage("lua can not be protocol description.");
        return null;
    }

    private void writeIdent(StringBuffer sb, int ident_num) {
        for (; ident_num > 0; --ident_num) {
            sb.append(ident);
        }
    }

    private void writeExport(StringBuffer sb, HashMap<String, Object> export_items, int ident_num) {
        sb.append("// this file is generated by xresloader, please don't edit it.").append(getSystemEndl())
                .append(endl);

        String export_mode_name = ProgramOptions.getInstance().javascriptExport;
        EXPORT_MODE export_mode = EXPORT_MODE.GLOBAL;
        if (null != export_mode_name) {
            // nodejs mode
            if (export_mode_name.equalsIgnoreCase("nodejs")) {
                export_mode = EXPORT_MODE.NODEJS;
            } else if (export_mode_name.equalsIgnoreCase("amd")) {
                export_mode = EXPORT_MODE.AMD;
            }
        }

        // export mode header
        switch (export_mode) {
        case AMD: {
            sb.append("define({");
            break;
        }
        case NODEJS: {
            break;
        }
        default: {
            // 设置导出命名空间
            if (!ProgramOptions.getInstance().javascriptGlobalVar.trim().isEmpty()) {
                String idents[] = ProgramOptions.getInstance().javascriptGlobalVar.trim().split("\\.");
                for (int i = idents.length - 1; i >= 0; --i) {
                    if (idents[i].isEmpty()) {
                        continue;
                    }

                    HashMap<String, Object> new_container = new HashMap<String, Object>();
                    new_container.put(idents[i], export_items);
                    export_items = new_container;
                }
            }

            sb.append(String.format("(function(){%s", endl));
            // extend function
            sb.append(ident).append("var extend = function (dst, src) {").append(endl);
            sb.append(ident).append(ident).append("for (var k in src) {").append(endl);
            sb.append(ident).append(ident).append(ident).append("var v = src[k];").append(endl);
            sb.append(ident).append(ident).append(ident).append("if (undefined === dst[k] || 'object' != typeof(v)) {")
                    .append(endl);
            sb.append(ident).append(ident).append(ident).append(ident).append("dst[k] = v;").append(endl);
            sb.append(ident).append(ident).append(ident).append("} else {").append(endl);
            sb.append(ident).append(ident).append(ident).append(ident).append("if ('object' != typeof(dst[k])) {")
                    .append(endl);
            sb.append(ident).append(ident).append(ident).append(ident).append(ident).append("dst[k] = {};")
                    .append(endl);
            sb.append(ident).append(ident).append(ident).append(ident).append("}").append(endl);
            sb.append(ident).append(ident).append(ident).append(ident).append("extend(dst[k], v)").append(endl);
            sb.append(ident).append(ident).append(ident).append("}").append(endl);
            sb.append(ident).append(ident).append("}").append(endl);
            sb.append(ident).append("};").append(endl);

            sb.append(endl).append(ident).append("var local_data_set = null;").append(endl);
            break;
        }
        }

        // export mode content
        boolean is_first = true;
        for (Map.Entry<String, Object> item : export_items.entrySet()) {
            // export mode header
            switch (export_mode) {
            case AMD: {
                if (is_first) {
                    sb.append(endl);
                } else {
                    sb.append(",").append(endl);
                }

                writeIdent(sb, ident_num + 1);
                sb.append(item.getKey()).append(": ");
                writeData(sb, item.getValue(), 1);
                break;
            }
            case NODEJS: {
                sb.append(String.format("exports.%s = ", item.getKey()));
                writeData(sb, item.getValue(), 0);
                sb.append(";").append(endl);
                break;
            }
            default: {
                sb.append(String.format("%slocal_data_set = ", ident));
                writeData(sb, item.getValue(), 1);
                sb.append(";").append(endl);

                sb.append(ident).append(String.format("try { extend(window, { %s : local_data_set }); }",
                        JSONObject.quote(item.getKey()))).append(endl);
                sb.append(ident).append(String.format("catch(e) { extend(global, { %s : local_data_set }); }",
                        JSONObject.quote(item.getKey()))).append(endl);
                sb.append(endl);
                break;
            }
            }

            is_first = false;
        }

        // export mode footer
        switch (export_mode) {
        case AMD: {
            writeIdent(sb, ident_num);
            sb.append(endl).append("});").append(endl);
            break;
        }
        case NODEJS: {
            break;
        }
        default: {
            sb.append("})();").append(endl);
            break;
        }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeData(StringBuffer sb, Object data, int ident_num) {
        // null
        if (null == data) {
            sb.append("undefined");
            return;
        }

        // 数字
        // 枚举值已被转为Java Long，会在这里执行
        if (data instanceof Number) {
            sb.append(data.toString());
            return;
        }

        // 布尔
        if (data instanceof Boolean) {
            sb.append(((Boolean) data) ? "true" : "false");
            return;
        }

        // 字符串&二进制
        if (data instanceof String) {
            // 利用json的字符串格式，和javascript一样的没必要再引入一个库
            sb.append(JSONObject.quote((String) data));
            return;
        }

        // 列表
        if (data instanceof List) {
            List<Object> ls = (List<Object>) data;
            sb.append("[");

            boolean is_first = true;
            for (Object obj : ls) {
                if (is_first) {
                    sb.append(endl);
                } else {
                    sb.append(",").append(endl);
                }

                writeIdent(sb, ident_num + 1);
                writeData(sb, obj, ident_num + 1);
                is_first = false;
            }

            if (!is_first) {
                sb.append(endl);
                writeIdent(sb, ident_num);
            }
            sb.append("]");
            return;
        }

        // Hashmap
        if (data instanceof Map) {
            Map<String, Object> mp = (Map<String, Object>) data;
            ArrayList<Map.Entry<String, Object>> sorted_array = new ArrayList<Map.Entry<String, Object>>();
            sorted_array.ensureCapacity(mp.size());
            sorted_array.addAll(mp.entrySet());
            sorted_array.sort((l, r) -> {
                if (l.getValue() instanceof Integer && r.getValue() instanceof Integer) {
                    return ((Integer) l.getValue()).compareTo((Integer) r.getValue());
                }

                return l.getKey().compareTo(r.getKey());
            });

            sb.append("{");

            boolean is_first = true;
            for (Map.Entry<String, Object> item : sorted_array) {
                if (is_first) {
                    sb.append(endl);
                } else {
                    sb.append(",").append(endl);
                }

                writeIdent(sb, ident_num + 1);
                sb.append(item.getKey()).append(" : ");

                writeData(sb, item.getValue(), ident_num + 1);
                is_first = false;
            }

            if (!is_first) {
                sb.append(endl);
                writeIdent(sb, ident_num);
            }
            sb.append("}");
            return;
        }

        sb.append(JSONObject.quote(data.toString()));
    }

    /**
     * 转储常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        init();

        StringBuffer sb = new StringBuffer();
        writeExport(sb, data, 0);
        sb.append(endl);

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    }
}
