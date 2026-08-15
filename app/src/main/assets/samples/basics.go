// Go：Google推出的现代语言，简洁高效，适合后端与云计算。
// 本示例覆盖：变量、函数、结构体、方法、接口、goroutine、
//   channel、select、defer/panic/recover、iota、错误处理。

package main

import (
	"errors"
	"fmt"
	"sync"
	"time"
)

// ── 1. 自定义类型与结构体 ──────────────────────────────
type User struct {
	ID    int
	Name  string
	Email string
	Age   int
}

// 为 User 添加方法（接收者类型前置）
func (u User) Greet() string {
	return fmt.Sprintf("你好，我是 %s，今年 %d 岁", u.Name, u.Age)
}

// 指针接收者：可修改接收者字段
func (u *User) Birthday() {
	u.Age++ // 指针接收者可以修改原值
}

// ── 2. 接口（interface） ──────────────────────────────
// 接口是隐式实现的——只要实现了接口里的所有方法就算实现
type Speaker interface {
	Speak() string
}

type Dog struct {
	Name string
}

// Dog 实现了 Speaker 接口（无需显式声明）
func (d Dog) Speak() string {
	return fmt.Sprintf("%s 说：汪！", d.Name)
}

type Cat struct {
	Name string
}

func (c Cat) Speak() string {
	return fmt.Sprintf("%s 说：喵～", c.Name)
}

// ── 3. 错误处理 ───────────────────────────────────────
// Go 没有 try-catch，函数返回 error 表示错误
func divide(a, b float64) (float64, error) {
	if b == 0.0 {
		return 0, errors.New("除数不能为零") // 返回新错误
	}
	return a / b, nil // nil 表示无错误
}

// 自定义错误类型
type ValidationError struct {
	Field string
	Msg   string
}

func (e *ValidationError) Error() string {
	return fmt.Sprintf("校验失败 [%s]: %s", e.Field, e.Msg)
}

// ── 4. iota（枚举/常量生成器） ─────────────────────────
// iota 在 const 块中每行自动递增，从 0 开始
const (
	StatusPending   = iota // 0
	StatusActive           // 1
	StatusInactive         // 2
	StatusDeleted          // 3
)

// iota 高级用法：位移枚举
const (
	FlagRead    = 1 << iota // 1
	FlagWrite               // 2
	FlagExecute             // 4
)

// ── 5. goroutine 与 channel ──────────────────────────
// goroutine：轻量级并发，go 关键字启动
func worker(id int, jobs <-chan int, results chan<- int) {
	for job := range jobs { // range channel 直到 close 为止
		fmt.Printf("worker %d 处理任务 %d\n", id, job)
		time.Sleep(100 * time.Millisecond) // 模拟耗时
		results <- job * 2
	}
}

// ── 6. select 多路复用 ────────────────────────────────
func readWithTimeout(ch <-chan int, timeout time.Duration) {
	select {
	case val := <-ch:
		fmt.Printf("收到值: %d\n", val)
	case <-time.After(timeout):
		fmt.Println("超时！")
	}
}

// ── 7. defer 与 panic/recover ────────────────────────
// defer 将函数调用压入栈，在函数返回前执行（LIFO）
func dataAccess() {
	defer func() {
		// recover 捕获 panic，防止程序崩溃
		if r := recover(); r != nil {
			fmt.Printf("捕获到 panic: %v\n", r)
		}
	}()
	fmt.Println("开始操作...")
	// panic("发生了严重错误") // 取消注释可触发 panic
	fmt.Println("正常结束")
}

// ── 8. 泛型（Go 1.18+） ───────────────────────────────
// Comparable 约束：可比较类型
func Max[T int | float64](a, b T) T {
	if a > b {
		return a
	}
	return b
}

// ── 9. map / slice ────────────────────────────────────
// slice：动态数组
func processItems(items []string) []string {
	result := make([]string, 0, len(items))
	for _, item := range items {
		if item != "" {
			result = append(result, item)
		}
	}
	return result
}

// ── main ──────────────────────────────────────────────
func main() {
	// 延迟执行（LIFO：后进先出）
	defer fmt.Println("=== 程序结束 ===") // 最后执行
	defer fmt.Println("清理资源...")        // 倒数第二

	// ── 变量声明 ──
	// var 声明 + 零值
	var count int // 默认 0
	// 短声明 :=（类型推断）
	name := "Go 语言"
	// 多变量声明
	x, y := 10, 20

	fmt.Printf("count=%d, name=%s, x=%d, y=%d\n", count, name, x, y)

	// ── 条件 ──
	if x > 5 {
		fmt.Println("x 大于 5")
	} else if x < 0 {
		fmt.Println("x 是负数")
	} else {
		fmt.Println("x 在 0-5 之间")
	}

	// if 中可包含一条语句（分号分隔）
	if val := x * 2; val > 15 {
		fmt.Printf("x*2=%d > 15\n", val)
	}

	// ── switch ──
	switch status := StatusActive; status {
	case StatusPending:
		fmt.Println("待处理")
	case StatusActive:
		fmt.Println("已激活")
	default:
		fmt.Println("未知")
	}

	// ── for 循环 ──
	// Go 只有 for 一种循环
	for i := 0; i < 5; i++ {
		fmt.Printf("%d ", i)
	}
	fmt.Println()

	// for 模拟 while
	j := 0
	for j < 3 {
		fmt.Printf("while: %d ", j)
		j++
	}
	fmt.Println()

	// 无限循环（使用 break 退出）
	n := 0
	for {
		n++
		if n > 5 {
			break
		}
	}
	fmt.Printf("无限循环执行了 %d 次\n", n)

	// ── 结构体 ──
	// 按字段名初始化（推荐）
	user := User{
		ID:    1,
		Name:  "Alice",
		Email: "alice@example.com",
		Age:   25,
	}
	fmt.Println(user.Greet())

	// 指针接收者方法
	user.Birthday()
	fmt.Printf("生日后: %d 岁\n", user.Age)

	// ── 接口 ──
	// 多态：不同类型共用一个接口
	speakers := []Speaker{
		Dog{Name: "Rex"},
		Cat{Name: "Kitty"},
	}
	for _, s := range speakers {
		fmt.Println(s.Speak())
	}

	// 空接口 interface{} / any：可接收任何类型
	var anything interface{} = "可以是任何类型"
	fmt.Println(anything)

	// ── 错误处理 ──
	result, err := divide(10, 0)
	if err != nil {
		fmt.Printf("错误: %v\n", err)
	} else {
		fmt.Printf("结果: %.2f\n", result)
	}

	// ── iota ──
	fmt.Printf("StatusActive = %d, FlagWrite = %d\n", StatusActive, FlagWrite)

	// ── Map ──
	scores := map[string]int{"Alice": 95, "Bob": 87}
	scores["Charlie"] = 92
	// 查询 + 判断 key 是否存在
	if score, ok := scores["Bob"]; ok {
		fmt.Printf("Bob 的分数: %d\n", score)
	}
	delete(scores, "Charlie") // 删除键
	fmt.Printf("Map 大小: %d\n", len(scores))

	// ── Slice (动态数组) ──
	nums := []int{1, 2, 3}
	nums = append(nums, 4, 5)     // append 追加
	fmt.Printf("slice: %v, len=%d, cap=%d\n", nums, len(nums), cap(nums))

	// make 创建指定容量
	buf := make([]int, 0, 10)     // len=0, cap=10

	// ── Range 遍历 ──
	for i, v := range nums {
		fmt.Printf("索引%d:值%d  ", i, v)
	}
	fmt.Println()

	// ── goroutine + channel ──
	jobs := make(chan int, 5)
	results := make(chan int, 5)

	// 启动 3 个 worker
	for w := 1; w <= 3; w++ {
		go worker(w, jobs, results)
	}

	// 发送任务
	for n := 1; n <= 5; n++ {
		jobs <- n
	}
	close(jobs) // 关闭后 range 会退出

	// 收集结果
	var total int
	for i := 0; i < 5; i++ {
		total += <-results
	}
	fmt.Printf("处理完毕，结果总和: %d\n", total)

	// ── select 示例 ──
	ch := make(chan int)
	go func() {
		time.Sleep(50 * time.Millisecond)
		ch <- 42
	}()
	readWithTimeout(ch, 100*time.Millisecond)

	// ── defer + recover ──
	dataAccess()

	// ── 泛型 ──
	fmt.Printf("Max(3,7)=%d, Max(3.1,2.9)=%.1f\n", Max(3, 7), Max(3.1, 2.9))

	// ── WaitGroup 等待所有 goroutine 完成 ──
	var wg sync.WaitGroup
	for i := 1; i <= 3; i++ {
		wg.Add(1)
		go func(id int) {
			defer wg.Done()
			time.Sleep(50 * time.Millisecond)
			fmt.Printf("WaitGroup worker %d 完成\n", id)
		}(i)
	}
	wg.Wait()
	fmt.Println("所有 worker 已完成")
}
