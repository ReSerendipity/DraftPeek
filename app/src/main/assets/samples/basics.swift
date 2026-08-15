// Swift：Apple推出的现代语言，用于iOS/macOS应用开发。
// 本示例覆盖：变量、函数、类、结构体、协议、扩展、
//   枚举、闭包、错误处理、并发、泛型、属性包装器、Codable。

import Foundation

// ── 1. 协议（Protocol） ───────────────────────────────
protocol Drawable {
    func draw()
}

protocol Identifiable {
    var id: String { get }           // 只读属性
}

// ── 2. 结构体（值类型） vs 类（引用类型） ──────────────
struct Point {
    var x: Double
    var y: Double

    // 计算属性：不存储值，每次计算
    var distanceFromOrigin: Double {
        sqrt(x * x + y * y)
    }

    // mutating：结构体的方法如需修改属性，必须标记 mutating
    mutating func moveBy(dx: Double, dy: Double) {
        x += dx
        y += dy
    }
}

struct User: Identifiable {
    let id: String
    var name: String
    var email: String?

    // 自定义 getter/setter
    var displayName: String {
        get { name }
        set { name = newValue.uppercased() }
    }
}

// ── 3. 类（引用类型） ─────────────────────────────────
class Shape: Drawable {
    var name: String

    init(name: String) {            // 指定初始化器
        self.name = name
    }

    // convenience 初始化器：必须调用本类的指定初始化器
    convenience init() {
        self.init(name: "未命名")
    }

    func draw() {
        print("Drawing \(name)")
    }
}

// 继承
class Circle: Shape {
    let radius: Double

    // 指定初始化器
    init(name: String, radius: Double) {
        self.radius = radius
        super.init(name: name)      // 调用父类初始化器
    }

    // 便利初始化器
    convenience override init(name: String) {
        self.init(name: name, radius: 1.0)
    }

    // 计算属性
    var area: Double {
        .pi * radius * radius
    }

    // property observer
    override var name: String {
        willSet { print("即将改为 \(newValue)") }
        didSet  { print("旧值 \(oldValue) 已改为 \(name)") }
    }

    override func draw() {
        print("Drawing circle: \(name), area: \(area)")
    }
}

// ── 4. 扩展（Extension） ──────────────────────────────
extension String {
    // 添加计算属性
    var firstLetter: Character? {
        first
    }

    // 添加方法
    func hello() -> String {
        "Hello, \(self)!"
    }
}

// ── 5. 枚举 ───────────────────────────────────────────
enum Result<T> {
    case success(T)
    case failure(Error)
}

enum Direction: String, CaseIterable {
    case north = "北"
    case south = "南"
    case east  = "东"
    case west  = "西"
}

// ── 6. 属性包装器 ─────────────────────────────────────
@propertyWrapper
struct Clamped<Value: Comparable> {
    var value: Value
    let range: ClosedRange<Value>

    var wrappedValue: Value {
        get { value }
        set { value = min(max(newValue, range.lowerBound), range.upperBound) }
    }
}

struct GameSettings {
    @Clamped(range: 0...100)
    var volume: Int = 50
}

// ── 7. Codable（JSON 序列化/反序列化） ────────────────
struct Person: Codable {
    let name: String
    let age: Int
    let skills: [String]
}

// ── 8. 错误处理 ───────────────────────────────────────
enum FetchError: Error {
    case invalidURL
    case noConnection
    case timeout
}

func fetchData(from url: String) throws -> String {
    guard !url.isEmpty else {
        throw FetchError.invalidURL
    }
    return "Data from \(url)"
}

// ── 9. 泛型 ──────────────────────────────────────────
func firstElement<T>(of array: [T]) -> T? {
    array.first
}

// ── 全局代码 ──────────────────────────────────────────
let circle = Circle(name: "MyCircle", radius: 5.0)
print("Circle area: \(circle.area)")
circle.draw()
circle.name = "RenamedCircle"

let greeting = "Swift".hello()
print(greeting)

let point = Point(x: 3, y: 4)
print("Distance from origin: \(point.distanceFromOrigin)")

var settings = GameSettings()
settings.volume = 150               // 被 Clamped 限制在 0...100
print("Volume: \(settings.volume)") // 输出 100

// ── 枚举 ──
let dir = Direction.north
print("方向: \(dir.rawValue)")

// ── Optional 处理 ──
var optionalName: String? = "Alice"
// if let 绑定
if let name = optionalName {
    print("Hello, \(name)")
}
// guard let 提前退出模式
func greet(_ name: String?) {
    guard let name = name else {
        print("名字为空")
        return
    }
    print("你好, \(name)")
}
greet(optionalName)

// nil coalescing (??) 合并运算符
let display = optionalName ?? "匿名"
print("显示: \(display)")

// ── 闭包（Closure） ──
let numbers = [-2, -1, 0, 1, 2, 3]
let positives = numbers.filter { $0 > 0 }
let doubled = numbers.map { $0 * 2 }
let sum = numbers.reduce(0, +)
print("Positives: \(positives), Doubled: \(doubled), Sum: \(sum)")

// ── 错误处理 do-catch ──
do {
    let data = try fetchData(from: "https://api.example.com")
    print(data)
} catch FetchError.invalidURL {
    print("无效 URL")
} catch {
    print("其他错误: \(error)")
}

// ── try? 返回 Optional ──
let optionalData = try? fetchData(from: "")
print("try? 结果: \(optionalData ?? "nil")")

// ── Codable ──
let jsonString = """
{
  "name": "Alice",
  "age": 30,
  "skills": ["Swift", "iOS", "Combine"]
}
"""
let jsonData = jsonString.data(using: .utf8)!
if let person = try? JSONDecoder().decode(Person.self, from: jsonData) {
    print("Decoded: \(person.name), age=\(person.age), skills=\(person.skills)")
}

// Encoding
if let encoded = try? JSONEncoder().encode(person!) {
    print(String(data: encoded, encoding: .utf8)!)
}

// ── 泛型 ──
print("First element: \(firstElement(of: ["a", "b", "c"]) ?? "nil")")

// ── 并发 async/await ──
Task {
    do {
        try await Task.sleep(nanoseconds: 1_000_000_000)
        print("并发任务完成")
    }
}
