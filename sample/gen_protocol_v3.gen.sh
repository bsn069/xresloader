#!/bin/bash
"D:/workspace/github/xresloader/xresloader/tools/windows_x86_64/protoc.exe" "-o" "./proto_v3/kind.pb" "--cpp_out" "D:/workspace/github/xresloader/xresloader/sample/cxx/v3" "-I" "./proto_v3" "-I" "../header/extensions/v3" "-I" "../header/extensions" "./proto_v3/dependency.proto" "./proto_v3/dep_level2.proto" "./proto_v3/kind.proto" "../header/extensions/google/protobuf/descriptor.proto" "../header/extensions/v3/xresloader.proto" "../header/extensions/v3/xresloader_ue.proto"