
使用 CURL 请求沙箱进行测试沙箱是否正常
```curl
curl -X POST "http://localhost:8090/executeCode" \
     -H "Content-Type: application/json" \
     -d '{
           "code": "public class Main {\n    public static void main(String[] args) { System.out.println(\"结果: hello world\");\n    }\n}\n",
           "language": "java"
         }'

```

IDEA Client

```
###
POST http://localhost:8090/executeCode
Content-Type: application/json

{
  "code": "public class Main {\n    public static void main(String[] args) {\n        int a = Integer.parseInt(args[0]);\n        int b = Integer.parseInt(args[1]);\n        System.out.println(\"结果:\" + (a + b));\n    }\n}",
  "language": "java",
  "inputList": ["1", "2"]
}

```
