// Rust：注重安全和性能的现代语言，无垃圾回收，零成本抽象。
// 本示例覆盖：变量、所有权、借用、生命周期、结构体、枚举、
//   模式匹配、Trait、泛型、错误处理、闭包、迭代器、智能指针。

use std::fmt;

// ── 1. 枚举（enum）与模式匹配 ─────────────────────────
#[derive(Debug)]
enum Status {
    Active,
    Inactive,
    Pending,
}

// 带数据的枚举
#[derive(Debug)]
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(u8, u8, u8),   // RGB
}

// ── 2. 结构体（struct） ──────────────────────────────
#[derive(Debug, Clone)]           // derive 宏自动生成 trait 实现
struct User {
    id: u32,
    username: String,             // String 拥有字符串所有权
    email: Option<String>,        // Option 表示可为空
    active: bool,
}

// 元组结构体
#[derive(Debug)]
struct Point(i32, i32);

// 单元结构体（无字段）
struct AlwaysEqual;

// ── 3. Trait（类似接口） ──────────────────────────────
trait Greeter {
    fn greet(&self) -> String;
}

// 为 User 实现 Greeter trait
impl Greeter for User {
    fn greet(&self) -> String {
        format!("Hello, {}!", self.username)
    }
}

// 在 User 上定义关联方法和实例方法
impl User {
    // 关联函数（类似静态方法），用 :: 调用
    fn new(id: u32, name: &str) -> Self {
        User {
            id,
            username: name.to_string(),
            email: None,
            active: true,
        }
    }

    // 方法（带 &self 参数），用 . 调用
    fn deactivate(&mut self) {    // &mut self 可变借用
        self.active = false;
    }
}

// ── 4. 泛型函数与 Trait 约束 ─────────────────────────
fn largest<T: PartialOrd>(list: &[T]) -> &T {
    let mut largest = &list[0];
    for item in list {
        if item > largest {
            largest = item;
        }
    }
    largest
}

// ── 5. 生命周期标注 ────────────────────────────────────
// 'a 表示返回值生命周期与较短参数相同
fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {
    if x.len() > y.len() { x } else { y }
}

// ── 6. 错误处理 —— Result ────────────────────────────
fn divide(a: i32, b: i32) -> Result<i32, String> {
    if b == 0 {
        Err("不允许除以零".to_string())
    } else {
        Ok(a / b)
    }
}

// ── 7. 闭包（Closure） ────────────────────────────────
// |参数| { 函数体 } —— 类型由编译器自动推断

// ── 8. 迭代器（Iterator） ────────────────────────────
fn process_numbers(numbers: &[i32]) -> Vec<i32> {
    numbers.iter()                 // 创建迭代器
        .filter(|&&n| n > 0)      // 保留正数
        .map(|&n| n * 2)          // 每个元素乘2
        .collect()                // 收集回 Vec
}

// ── main ──────────────────────────────────────────────
fn main() {
    // ── 变量绑定（let，默认不可变） ──
    let x = 5;                     // x 不可变
    // x = 6;                      // 编译错误！
    let mut y = 10;                // mut 关键字允许修改
    y += 1;                        // 可变引用修改

    // 常量（编译期求值，必须标注类型）
    const MAX_POINTS: u32 = 100_000;

    // ── 条件与循环 ──
    let condition = true;
    let num = if condition { 5 } else { 6 };  // if 是表达式，可赋值

    // for 循环遍历
    let fruits = vec!["apple", "banana", "cherry"];
    for (idx, fruit) in fruits.iter().enumerate() {
        println!("{}: {}", idx, fruit);
    }

    // while 循环
    let mut count = 3;
    while count > 0 {
        println!("countdown: {}", count);
        count -= 1;
    }

    // loop 无限循环
    let mut n = 0;
    let result = loop {
        n += 1;
        if n == 10 { break n * 2; }  // break 可返回值
    };
    println!("Loop result: {}", result);

    // ── 所有权（Ownership） ──
    let s1 = String::from("hello");
    let s2 = s1;                   // s1 所有权转移到 s2，s1 失效
    // println!("{}", s1);         // 编译错误：s1 已移动

    // ── 借用（Borrowing） ──
    let s3 = String::from("world");
    let len = calculate_length(&s3); // & 不可变借用
    println!("{} 的长度是 {}", s3, len);

    // ── 可变借用 ──
    let mut s4 = String::from("hello");
    append_world(&mut s4);         // &mut 可变借用
    println!("{}", s4);
    // 同一时间只能有一个可变借用或多个不可变借用，不能混用

    // ── 切片（Slice） ──
    let arr = [1, 2, 3, 4, 5];
    let slice = &arr[1..4];        // 索引 1,2,3
    println!("slice: {:?}", slice);

    let s = String::from("hello world");
    let word = first_word(&s);     // 字符串切片 &str
    println!("第一个单词: {}", word);

    // ── 结构体 ──
    let mut user = User::new(1, "Alice");
    user.email = Some("alice@example.com".to_string());  // Some 包装值
    println!("{}", user.greet());  // trait 方法

    // ── 枚举与模式匹配 ──
    let msg = Message::Write("Hi!".to_string());
    match msg {
        Message::Quit => println!("退出"),
        Message::Move { x, y } => println!("移动到 ({}, {})", x, y),
        Message::Write(text) => println!("写入: {}", text),
        Message::ChangeColor(r, g, b) => println!("颜色: ({},{},{})", r, g, b),
    }

    // if let 语法糖（只匹配一种模式）
    if let Message::Write(t) = Message::Write("简洁".into()) {
        println!("if let: {}", t);
    }

    // ── Option 处理 ──
    let maybe_number: Option<i32> = Some(42);
    match maybe_number {
        Some(n) => println!("有值: {}", n),
        None => println!("无值"),
    }

    // ── Error 处理 ──
    match divide(10, 2) {
        Ok(v) => println!("结果: {}", v),
        Err(e) => println!("错误: {}", e),
    }
    // unwrap 取出值，如果是 Err 则 panic（仅示意）
    println!("divide unwrap: {}", divide(10, 2).unwrap());
    // ? 操作符：如果是 Err 则提前返回（需在返回 Result 的函数中使用）

    // ── 闭包 ──
    let add_one = |x: i32| x + 1;
    println!("闭包: {}", add_one(5));

    // ── 迭代器 ──
    let nums = vec![-2, -1, 0, 1, 2, 3];
    let processed = process_numbers(&nums);
    println!("处理后: {:?}", processed);

    // ── 泛型 ──
    let numbers = vec![34, 50, 25, 100, 65];
    println!("最大: {}", largest(&numbers));

    // ── 生命周期 ──
    let string1 = String::from("abcd");
    let string2 = "xyz";
    println!("最长: {}", longest(string1.as_str(), string2));

    // ── 智能指针 Box ──
    // Box 将数据放在堆上
    let b = Box::new(5);
    println!("Box: {}", *b);       // * 解引用
}

// 辅助函数：演示引用与所有权
fn calculate_length(s: &String) -> usize {
    s.len()                        // 借用不转移所有权
}

fn append_world(s: &mut String) {
    s.push_str(" world!");         // 可变借用：可修改
}

fn first_word(s: &str) -> &str {
    let bytes = s.as_bytes();
    for (i, &item) in bytes.iter().enumerate() {
        if item == b' ' {
            return &s[..i];        // 返回切片
        }
    }
    &s[..]
}
