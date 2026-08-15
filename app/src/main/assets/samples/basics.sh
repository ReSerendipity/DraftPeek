#!/bin/bash
# Bash：Linux/Unix 脚本语言，用于系统管理和自动化操作。
# 本示例覆盖：变量、条件、循环、函数、数组、字符串操作、
#   管道、重定向、信号处理、算术、进程替换、Here文档。

set -euo pipefail                # 严格模式：遇错退出、未定义变量报错、管道错误
# set -x                         # 调试模式：打印每条命令

# ── 1. 变量 ─────────────────────────────────────────────
NAME="Alice"                     # 赋值等号两侧不能有空格
AGE=30
readonly PI=3.14159              # 只读变量（不可修改）

# 变量引用：$var 或 ${var}（推荐用花括号防止歧义）
echo "Hello, $NAME! Age: $AGE"
echo "Hello, ${NAME}!"

# 命令替换：$(command) 或 `command`（推荐 $()，支持嵌套）
CURRENT_DATE=$(date +"%Y-%m-%d %H:%M:%S")
echo "当前时间: $CURRENT_DATE"

# ── 2. 字符串操作 ────────────────────────────────────────
STR="Hello World"
echo "长度: ${#STR}"              # 字符串长度 → 11
echo "截取: ${STR:0:5}"           # 截取 0-4 → Hello
echo "替换: ${STR/World/Bash}"    # 替换首次匹配 → Hello Bash
echo "全替换: ${STR//o/O}"        # 替换所有 o → HellO WOrld
echo "删除前缀: ${STR#Hello }"    # 删除最短匹配前缀 → World
echo "删除后缀: ${STR%World}"     # 删除最短匹配后缀 → "Hello "

# ── 3. 数组 ─────────────────────────────────────────────
# 定义数组（空格分隔）
TAGS=("admin" "user" "editor" "guest")
echo "第一个: ${TAGS[0]}"         # 访问元素（索引从0开始）
echo "全部: ${TAGS[@]}"           # 展开所有元素
echo "个数: ${#TAGS[@]}"          # 数组长度 → 4
# 追加元素
TAGS+=("moderator")
# 切片
echo "切片(1-2): ${TAGS[@]:1:2}"  # 从索引1取2个
# 删除元素
unset 'TAGS[3]'                   # 删除索引3的元素

# ── 4. 条件判断 ─────────────────────────────────────────
# [ ] 和 [[ ]] 的区别：[[ ]] 功能更强（支持正则、不需引号保护）
if [[ "$AGE" -ge 18 ]]; then
    echo "成人"
elif [[ "$AGE" -ge 13 ]]; then
    echo "青少年"
else
    echo "儿童"
fi

# 字符串比较
if [[ "$NAME" == "Alice" ]]; then echo "是Alice"; fi
if [[ "$NAME" != "Bob" ]];    then echo "不是Bob"; fi
# 正则匹配（[[ ]] 专有）
if [[ "$NAME" =~ ^A.* ]];     then echo "以A开头"; fi

# 文件测试
if [[ -f "/etc/hosts" ]]; then echo "hosts 文件存在"; fi  # -f 是否为普通文件
if [[ -d "/tmp" ]];       then echo "/tmp 目录存在"; fi   # -d 是否为目录
if [[ -x "/bin/bash" ]];  then echo "bash 可执行"; fi     # -x 是否可执行
if [[ -s "/etc/hosts" ]]; then echo "hosts 非空"; fi      # -s 是否非空

# 数字比较：-eq(等于) -ne(不等) -lt(小于) -le(≤) -gt(大于) -ge(≥)
# 字符串比较：=(等于) !=(不等) <(小于) >(大于) -z(空) -n(非空)

# ── 5. 循环 ─────────────────────────────────────────────
# for 循环：遍历列表
for tag in "${TAGS[@]}"; do
    echo "Tag: $tag"
done

# C 风格 for
for ((i = 1; i <= 5; i++)); do
    echo -n "$i "
done
echo

# while 循环
NUMBERS=(1 2 3 4 5 6 7 8 9 10)
EVENS=()
idx=0
while [[ $idx -lt ${#NUMBERS[@]} ]]; do
    if (( NUMBERS[idx] % 2 == 0 )); then
        EVENS+=("${NUMBERS[idx]}")
    fi
    ((idx++))
done
echo "偶数: ${EVENS[*]}"

# until 循环（条件为假时执行，为真时退出）
count=3
until [[ $count -le 0 ]]; do
    echo "倒计时: $count"
    ((count--))
done

# ── 6. case 语句 ────────────────────────────────────────
read -r -p "输入选择 (start/stop/restart): " CHOICE
case "$CHOICE" in
    start)
        echo "启动服务..."
        ;;
    stop)
        echo "停止服务..."
        ;;
    restart|reload)               # 多值匹配
        echo "重启服务..."
        ;;
    *)
        echo "未知操作: $CHOICE"
        ;;
esac

# ── 7. 函数 ─────────────────────────────────────────────
greet() {
    local name="$1"              # local 声明局部变量
    local age="${2:-未知}"       # 第二个参数默认值
    echo "你好，$name！年龄：$age"
    return 0                     # 返回值（0=成功，1-255=错误码）
}

greet "Alice" 30                 # 函数调用，空格分隔参数
result=$(greet "Bob")            # 捕获函数输出
echo "$result"

# ── 8. 管道与重定向 ─────────────────────────────────────
# >  覆盖写入文件
# >> 追加写入文件
# 2> 重定向标准错误
# &> 重定向标准输出+标准错误
# <  从文件读取
echo "Hello" > output.txt
echo "World" >> output.txt       # 追加
cat output.txt

# 管道：前一个命令的输出作为后一个命令的输入
ls -la | grep "\.txt" | wc -l    # 统计 txt 文件数量

# 重定向输入
while IFS= read -r line; do
    echo "行: $line"
done < output.txt

# ── 9. 算术运算 ─────────────────────────────────────────
# 整数运算用 (()) 或 $(( ))
a=10; b=3
sum=$((a + b))                   # 13
div=$((a / b))                   # 3（整数除法）
mod=$((a % b))                   # 1
pow=$((a ** 2))                  # 100
echo "计算: ${a}+${b}=$sum, ${a}**2=$pow"

# 浮点运算用 bc / awk
float_result=$(echo "scale=2; 10 / 3" | bc)
echo "10/3=$float_result"

# ── 10. 进程替换与 Here 文档 ────────────────────────────
# 进程替换 <(cmd)：把命令输出临时当文件用
diff <(echo "line1") <(echo "line2") || true

# Here 文档：将多行文本传给命令
cat <<EOF > config.txt
# 配置文件
name=$NAME
age=$AGE
EOF

# Here 字符串：将字符串直接给 stdin
grep "Alice" <<< "$NAME is here" || true

# ── 11. 信号处理（trap） ────────────────────────────────
cleanup() {
    echo "清理临时文件..."
    rm -f output.txt config.txt
}
trap cleanup EXIT                 # 脚本退出时自动执行 cleanup

# ── 12. 其他常用操作 ────────────────────────────────────
# 后台运行 && 等待
sleep 1 &
pid=$!
echo "后台进程 PID: $pid"
wait "$pid"                      # 等待后台进程完成

# 检查上一条命令的退出状态（$? = 0 表示成功）
if ls /nonexistent 2>/dev/null; then
    echo "存在"
else
    echo "不存在（退出码: $?）" # 退出状态码
fi

# 默认值替换
echo "${UNDEFINED:-默认值}"      # 如果未定义则用默认值
echo "${UNDEFINED:=赋值}"        # 如果未定义则赋值并返回
# ${var:?错误消息}                # 如果未定义则报错退出

# 生成序列
echo "序列: $(seq 1 5)"

echo "脚本执行完毕"
