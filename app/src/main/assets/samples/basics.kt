/*
 * ═══════════════════════════════════════════════════════════════════════════════
 *  项目名称：DraftPeek（撰码轻览）
 *  ────────────────────────────────────────────────────────────────────────────
 *  文件说明：这是 DraftPeek 代码编辑器的 Kotlin 语言基础语法示例文件
 *  ────────────────────────────────────────────────────────────────────────────
 *  主要功能：展示 Kotlin 语言的核心语法特性，供编辑器语法高亮测试和用户学习参考
 *  ────────────────────────────────────────────────────────────────────────────
 *  核心技术栈：Kotlin 协程、Flow、泛型、密封类、数据类、委托属性
 *  ────────────────────────────────────────────────────────────────────────────
 *  适用场景：sora-editor 语法高亮验证、TextMate 语法测试、Tree-sitter 解析测试
 *  ────────────────────────────────────────────────────────────────────────────
 *  关键注意事项：本文件是演示用例，代码仅用于语法展示，非生产环境代码
 * ═══════════════════════════════════════════════════════════════════════════════
 */

// Kotlin：现代 JVM 语言，Android 官方推荐，语法简洁安全。
// 本示例覆盖：变量、数据类型、条件、循环、函数、类、
//   数据类、密封类、扩展函数、委托、泛型、协程、Flow。

// ── 导入语句说明 ──────────────────────────────────────
// kotlinx.coroutines.*：Kotlin 协程核心库，提供 launch/async/delay 等协程原语
// kotlinx.coroutines.flow.*：Flow 异步流库，提供响应式数据流支持
// kotlin.properties.Delegates：Kotlin 标准库委托属性，包含 observable/vetoable 等
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.properties.Delegates

// ── 1. 密封类：固定子类的继承 ──────────────────────────
// sealed class 限制继承层级，所有子类必须在同一文件内定义
// 编译器可确保 when 表达式覆盖所有子类，无需 else 分支
// out T 表示协变（生产者类型），Success 可持有具体类型 T
sealed class Result<out T> {
    // 成功状态：持有返回数据
    data class Success<T>(val data: T) : Result<T>()
    // 错误状态：持有错误信息和可选的异常原因
    // Nothing 是 Kotlin 底类型，表示永远不存在的值
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    // 加载状态：单例对象，无数据
    object Loading : Result<Nothing>()
}

// ── 2. 数据类：自动生成 equals/hashCode/toString/copy ──
// data class 专为存储数据设计，编译器自动生成：
// - equals()/hashCode()：基于属性的相等性判断
// - toString()：格式化输出所有属性
// - copy()：创建副本并可修改部分属性
// - componentN()：支持解构声明
data class User(
    val id: Int,                                    // 用户ID，不可变
    val name: String,                               // 用户名
    val email: String? = null,                      // 邮箱，可空类型，默认null
    val age: Int = 0                                // 年龄，默认0
)

// ── 3. 扩展函数：给已有类添加新函数 ────────────────────
// 无需继承或装饰器模式，直接为现有类添加成员函数
// 扩展函数是静态解析的，不会真正修改原类
fun String.hello(): String = "Hello, $this!"        // 为 String 类添加问候方法
fun User.greet(): String = "${this.name} says hi!" // 为 User 类添加问候方法

// ── 4. 委托属性 ──────────────────────────────────────
// Kotlin 委托属性将属性的 getter/setter 逻辑委托给另一个对象
// 语法：val/var 属性名: 类型 by 委托对象

// 懒加载示例：首次访问时才初始化
class LazyExample {
    // lazy() 函数返回 Lazy<T> 实例，默认线程安全（LazyThreadSafetyMode.SYNCHRONIZED）
    // 适合初始化成本高且可能不被使用的资源
    val hugeData: String by lazy {
        println("正在加载...")                      // 仅首次访问时执行
        "大数据加载完成"
    }
}

// 可观察属性示例：值变化时触发回调
class ObservableExample {
    // Delegates.observable() 在属性值变化后触发回调
    // 回调参数：property（属性元数据）、old（旧值）、new（新值）
    var name: String by Delegates.observable("初始值") { _, old, new ->
        println("$old -> $new")                     // 值变化时打印变更日志
    }
}

// ── 5. 单例 object ────────────────────────────────────
// object 关键字声明线程安全的单例，由 Kotlin 编译器保证
// 等价于 Java 的饿汉式单例，类加载时即初始化
object Config {
    const val APP_NAME = "DraftPeek"                // 编译期常量，应用名称
    const val VERSION = "1.0"                       // 编译期常量，版本号
}

// ── 6. 伴生对象（companion object） ────────────────────
// 伴生对象的成员可通过类名直接访问，类似 Java 的静态成员
// 但本质上是实例成员，可实现接口、使用扩展
class ApiClient private constructor() {             // 私有构造函数，禁止外部实例化
    companion object {
        const val BASE_URL = "https://api.example.com" // 基础URL，编译期常量
        private var instance: ApiClient? = null     // 单例实例，可空

        // 简单的懒汉式单例获取方法
        // also 函数：执行块内逻辑后返回对象本身
        fun getInstance(): ApiClient {
            return instance ?: ApiClient().also { instance = it }
        }
    }
}

// ── 7. 协程与 Flow ────────────────────────────────────
// suspend 关键字标记挂起函数，只能在协程或其他挂起函数中调用
// 挂起函数不会阻塞线程，而是挂起协程，释放线程供其他使用
suspend fun fetchData(): String {
    delay(1000)                                     // 非阻塞挂起1秒（模拟网络请求）
    return "Fetched data"
}

// Flow：冷数据流，只有调用 collect 时才开始执行
// 特性：
// - 冷流：每个收集者独立执行流
// - 背压：通过挂起机制自然支持
// - 按顺序发射：默认不并发
// - 可组合：支持丰富的操作符
fun countFlow(): Flow<Int> = flow {
    // flow 构建器创建冷流
    for (i in 1..5) {
        delay(200)                                  // 模拟异步间隔
        emit(i)                                     // 发射值到流中
    }
}

// ── 8. 内联函数 ───────────────────────────────────────
// inline 关键字让编译器在调用处内联函数体，避免函数调用开销
// 对于接受 lambda 参数的高阶函数，内联可避免创建额外对象
inline fun <T> measureTime(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()          // 记录开始时间
    val result = block()                            // 执行传入的代码块
    return result to (System.currentTimeMillis() - start) // 返回结果和耗时
}

// ── 9. 泛型协变/逆变 ────────────────────────────────
// Kotlin 泛型通过 out/in 关键字声明型变（Declaration-site variance）
// out T：协变（生产者），只能输出 T，不能输入 T → 支持子类型转父类型
// in T：逆变（消费者），只能输入 T，不能输出 T → 支持父类型转子类型

// 生产者接口：只能返回 T，不能接收 T 作为参数
interface Producer<out T> {
    fun produce(): T                                // T 作为返回值（输出位置）
}

// 消费者接口：只能接收 T 作为参数，不能返回 T
interface Consumer<in T> {
    fun consume(item: T)                            // T 作为参数（输入位置）
}

// ── main 函数 ──────────────────────────────────────────────
// runBlocking<Unit>：创建协程作用域并阻塞当前线程直到所有子协程完成
// Unit 表示返回值类型（类似 Java void）
// 用途：
// - 桥接阻塞世界和协程世界
// - main 函数和测试中常用
// - 生产代码应避免使用，应使用 CoroutineScope/Dispatchers
fun main() = runBlocking<Unit> {
    // ── 变量声明 ──
    val immutable = "不可变"                        // val（value）：只读引用，初始化后不可重新赋值（类似 Java final）
    var mutable = "可变"                            // var（variable）：可变引用，可重新赋值
    val nullable: String? = null                    // ? 标记可空类型，表示该变量可持有 null

    // 安全调用 ?. 和 Elvis 操作符 ?:
    // ?.：如果对象非空则调用，否则返回 null
    // ?:：如果左侧为 null 则使用右侧默认值（空安全默认值）
    println(nullable?.length ?: "为空")

    // ── 条件 when（加强版 switch） ──
    // when 是 Kotlin 的多功能条件表达式，可替代 if-else 链和 Java switch
    val score = 85
    val grade = when (score) {
        in 90..100 -> "A"                          // 范围匹配：90-100 为 A
        in 80..89  -> "B"                          // 范围匹配：80-89 为 B
        in 70..79  -> "C"                          // 范围匹配：70-79 为 C
        else       -> "D"                          // 其他情况为 D（必须有，除非编译器能证明穷尽）
    }
    println("Grade: $grade")

    // when 也可以不接参数，此时分支条件为任意布尔表达式
    when {
        score >= 90 -> println("优秀")
        score >= 60 -> println("及格")
        else        -> println("不及格")
    }

    // ── 循环 ──
    // Kotlin 没有传统 for 循环，使用范围表达式

    // 闭区间遍历：1 到 5（包含端点）
    for (i in 1..5) print("$i ")
    println()

    // downTo 递减：从 5 到 1
    for (i in 5 downTo 1) print("$i ")
    println()

    // step 步进：每次增加 2
    for (i in 1..10 step 2) print("$i ")
    println()

    // while 循环：条件满足时重复执行
    var count = 3
    while (count > 0) {
        print("w$count ")
        count--
    }
    println()

    // ── 集合操作 ──
    // Kotlin 集合分只读（List/Set/Map）和可变（MutableList/MutableSet/MutableMap）
    val numbers = listOf(1, 2, 3, 4, 5)            // 创建只读 List

    // 集合链式操作：函数式风格，每一步返回新集合，不修改原集合
    val doubled = numbers
        .filter { it % 2 == 0 }                     // 第1步：过滤偶数 → [2, 4]
        .map { it * it }                            // 第2步：平方变换 → [4, 16]
        .sortedDescending()                         // 第3步：降序排序 → [16, 4]
    println("Doubled evens: $doubled")

    // associate：将列表转换为 Map，lambda 返回键值对
    val nameMap = numbers.associate { it to "item$it" }
    println(nameMap)

    // groupBy：按指定键分组，返回 Map<Key, List<T>>
    val grouped = listOf("a", "ab", "abc", "b").groupBy { it.length }
    println("Grouped: $grouped")

    // ── 可空类型与安全操作 ──
    val maybeName: String? = "Alice"

    // let 作用域函数：对象非空时执行块内代码
    // it 指代对象本身，返回值为块内最后一行
    maybeName?.let { println("Hello $it") }

    // !! 非空断言运算符：强制将可空类型转为非空类型
    // 若值为 null 会抛出 NullPointerException，生产代码慎用
    println(maybeName!!.uppercase())

    // ── 范围表达式 ──
    // in 关键字检测值是否在范围内（.. 是闭区间，until 是左闭右开）
    val x = 5
    if (x in 1..10) println("在范围内")

    // ── 数据类 ──
    val user = User(1, "Alice", email = "alice@example.com")
    println(user)                                   // 自动生成的 toString()
    val older = user.copy(age = 31)                 // copy 创建副本，仅修改 age
    println(older)

    // ── 扩展函数 ──
    println("World".hello())                        // 调用 String 扩展函数
    println(user.greet())                           // 调用 User 扩展函数

    // ── 委托 ──
    val lazyExample = LazyExample()
    println(lazyExample.hugeData)                   // 首次访问：执行初始化块
    println(lazyExample.hugeData)                   // 后续访问：直接返回缓存值

    val obs = ObservableExample()
    obs.name = "第一次更改"                          // 触发 observable 回调
    obs.name = "第二次更改"

    // ── object / companion ──
    println("${Config.APP_NAME} v${Config.VERSION}") // 直接通过类名访问 object 成员
    println(ApiClient.BASE_URL)                     // 通过伴生对象访问，类似静态成员

    // ── 密封类 ──
    // 密封类配合 when 使用时