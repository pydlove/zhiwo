#!/bin/bash

# 替换为你的 MiniMax API Key
API_KEY="你的_MiniMax_API_Key"

# 测试 MiniMax API 连通性
echo "=== 测试 MiniMax API ==="
curl -v -m 30 \
  https://api.minimax.chat/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer ${API_KEY}" \
  -d '{
    "model": "MiniMax-M2.7",
    "messages": [
      {"role": "user", "content": "你好"}
    ]
  }'

echo ""
echo ""
echo "=== 测试原始长 prompt（与报错一致）==="
curl -v -m 300 \
  https://api.minimax.chat/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer ${API_KEY}" \
  -d '{
    "model": "MiniMax-M2.7",
    "messages": [
      {"role": "user", "content": "围绕\"朱元璋定下一条明代规矩：大臣上朝前必须吃饱饭，背后大有深意\"根据以下规则创作文章，\n生成规则：\n1、创作规则：根据\"朱元璋定下一条明代规矩：大臣上朝前必须吃饱饭，背后大有深意\"进行创作；\n2、每篇文章要求字数严禁少于900字，严禁超过1200字；\n3、文章头部严禁写上标题；\n4、输出文件样式：\n4.1、文章标题放在<h1></h1>标签中；\n4.2、章节标题放在<h3></h3>标签中；\n4.3、着重加强的放在<s></s>标签中；\n5、文笔风格：\n5.1、$；\n5.2、严禁标题排比，\n5.3、严禁用排比句和类比句，严禁一二三/123这种段落的数字，严禁内容中使用破折号、顿号和分号，严禁连续多个问句；\n5.4、写作框架严禁“总-分-总”；\n5.5、文中金句严禁超过1句；\n5.6、文中的比喻严禁超过3句；\n5.7、叙述适当留白，严禁逻辑上强烈的闭环；\n5.8、严禁句式长度过于对称，严禁文章结构性对称，严禁结构上的“完美对称”；\n5.9、严禁过度修饰，例如：“被帝国制度反复拉扯的疲惫面容”、“宏伟却空荡的大殿”；\n6.0、文章增加一点口语化的内容，例如：书面语是“每一次正式的临朝，都可能演变成道德审判的剧场”，改成的口语就是\n“每次上朝，搞不好就变成一场批斗大会”；"}
    ]
  }'
