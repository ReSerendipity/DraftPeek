// C#：微软开发的现代、面向对象、类型安全的编程语言，运行于.NET平台。
// 本示例覆盖：变量、数据类型、控制流、方法、类、接口、泛型、
//   LINQ、异步编程、集合、文件操作、字符串处理、异常处理、委托与事件。

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

// ── 1. 接口（interface） ──────────────────────────────
// 接口定义契约，类可以实现多个接口
interface IDrawable
{
    void Draw();
    string Name { get; }
}

// ── 2. 抽象类（abstract class） ───────────────────────
abstract class Shape
{
    public string Name { get; protected set; }

    protected Shape(string name)
    {
        Name = name;
    }

    // 抽象方法：派生类必须实现
    public abstract double Area();

    // 虚方法：派生类可以重写
    public virtual void PrintInfo()
    {
        Console.WriteLine($"{Name} area={Area():F2}");
    }
}

// ── 3. 类与继承 ───────────────────────────────────────
class Circle : Shape, IDrawable
{
    // 私有字段（封装）
    private double _radius;
    // 静态字段（所有实例共享）
    public static int CircleCount { get; private set; } = 0;

    // 构造函数
    public Circle(string name, double radius) : base(name)
    {
        _radius = radius;
        CircleCount++;
    }

    // 属性（Property）
    public double Radius
    {
        get => _radius;
        set => _radius = value > 0 ? value : throw new ArgumentException("半径必须大于0");
    }

    // 重写抽象方法
    public override double Area() => Math.PI * _radius * _radius;

    // 实现接口方法
    public void Draw()
    {
        Console.WriteLine($"Drawing circle: {Name}, radius={_radius}");
    }
}

// ── 4. 泛型类 ─────────────────────────────────────────
class Box<T>
{
    private T _item;

    public void Put(T item) => _item = item;
    public T Get() => _item;
}

// ── 5. 泛型方法与约束 ────────────────────────────────
static class Utils
{
    public static T First<T>(List<T> list)
    {
        if (list == null || list.Count == 0)
            throw new InvalidOperationException("列表为空");
        return list[0];
    }

    public static T Max<T>(T a, T b) where T : IComparable<T>
    {
        return a.CompareTo(b) > 0 ? a : b;
    }
}

// ── 6. 枚举 ───────────────────────────────────────────
enum Color
{
    Red,
    Green,
    Blue
}

// ── 7. 委托与事件 ─────────────────────────────────────
delegate void MessageHandler(string message);

class Notifier
{
    // 事件：基于委托
    public event MessageHandler OnMessage;

    public void SendMessage(string msg)
    {
        OnMessage?.Invoke(msg);
    }
}

// ── 主程序 ──────────────────────────────────────────
class Program
{
    static async Task Main(string[] args)
    {
        // ── 变量与常量 ──
        int count = 10;                  // 整型
        const double PI = 3.14159;       // const：编译时常量
        readonly string appName = "C#";  // readonly：运行时常量
        bool flag = true;
        char letter = 'A';
        string text = "Hello C#";

        // 可空类型（Nullable）
        int? nullableInt = null;
        string nullableStr = null;

        // 类型推断（var）
        var inferredNumber = 42;
        var inferredString = "type inferred";

        Console.WriteLine($"变量: count={count}, PI={PI}, flag={flag}");

        // ── 条件语句 ──
        if (count > 5)
        {
            Console.WriteLine("大于5");
        }
        else if (count > 0)
        {
            Console.WriteLine("1-5之间");
        }
        else
        {
            Console.WriteLine("非正数");
        }

        // switch 表达式（C# 8+）
        string day = "Mon";
        string dayName = day switch
        {
            "Mon" => "周一",
            "Tue" => "周二",
            "Fri" => "周五",
            _ => "其他"
        };
        Console.WriteLine($"Switch: {dayName}");

        // 模式匹配（is 表达式）
        object obj = "Hello Pattern Matching";
        if (obj is string s)
        {
            Console.WriteLine($"模式匹配: {s.ToUpper()}");
        }

        // ── 循环 ──
        // for 循环
        for (int i = 0; i < 3; i++)
        {
            Console.Write($"{i} ");
        }
        Console.WriteLine();

        // foreach 循环
        int[] arr = { 1, 2, 3, 4, 5 };
        foreach (int n in arr)
        {
            Console.Write($"{n} ");
        }
        Console.WriteLine();

        // while 循环
        int w = 0;
        while (w < 3)
        {
            Console.Write("w");
            w++;
        }
        Console.WriteLine();

        // do-while 循环
        int dw = 0;
        do
        {
            Console.Write("dw");
            dw++;
        } while (dw < 2);
        Console.WriteLine();

        // ── 数组 ──
        string[] names = { "Alice", "Bob", "Charlie" };
        int[,] matrix = { { 1, 2 }, { 3, 4 } };  // 二维数组

        // ── 面向对象 ──
        Shape circle = new Circle("MyCircle", 5.0);
        circle.PrintInfo();
        ((IDrawable)circle).Draw();
        Console.WriteLine($"Circles created: {Circle.CircleCount}");

        // ── 枚举 ──
        Color color = Color.Red;
        Console.WriteLine($"Color: {color} (value={(int)color})");

        // ── 集合（System.Collections.Generic） ──
        // List<T>：动态数组
        List<string> list = new List<string> { "A", "B", "C" };
        list.Add("D");
        list.Remove("B");

        // Dictionary<TKey, TValue>：键值对
        Dictionary<string, int> scores = new Dictionary<string, int>
        {
            ["Alice"] = 95,
            ["Bob"] = 87
        };
        foreach (var kvp in scores)
        {
            Console.WriteLine($"{kvp.Key}: {kvp.Value}");
        }

        // HashSet<T>：无序不重复集合
        HashSet<int> set = new HashSet<int> { 1, 2, 3, 2, 1 };
        Console.WriteLine($"Set count: {set.Count}");

        // Queue<T>：队列
        Queue<string> queue = new Queue<string>();
        queue.Enqueue("first");
        queue.Enqueue("second");
        Console.WriteLine($"Queue peek: {queue.Peek()}");

        // Stack<T>：栈
        Stack<int> stack = new Stack<int>();
        stack.Push(1);
        stack.Push(2);
        Console.WriteLine($"Stack pop: {stack.Pop()}");

        // ── LINQ（Language Integrated Query） ──
        List<int> numbers = new List<int> { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        // 查询语法
        var evensQuery = from n in numbers
                         where n % 2 == 0
                         orderby n descending
                         select n * n;

        // 方法语法（更常用）
        var evensMethod = numbers
            .Where(n => n % 2 == 0)
            .Select(n => n * n)
            .OrderByDescending(n => n)
            .ToList();

        Console.WriteLine($"LINQ evens squared: {string.Join(", ", evensMethod)}");

        // 聚合操作
        int sum = numbers.Sum();
        double avg = numbers.Average();
        int max = numbers.Max();
        int min = numbers.Min();
        Console.WriteLine($"Sum={sum}, Avg={avg:F1}, Max={max}, Min={min}");

        // 分组
        var groups = numbers.GroupBy(n => n % 2 == 0 ? "偶数" : "奇数");
        foreach (var group in groups)
        {
            Console.WriteLine($"{group.Key}: {string.Join(",", group)}");
        }

        // ── 字符串操作 ──
        string str1 = "Hello";
        string str2 = "World";
        string combined = string.Join(" ", str1, str2);
        Console.WriteLine($"Combined: {combined}");
        Console.WriteLine($"Upper: {combined.ToUpper()}");
        Console.WriteLine($"Contains 'World': {combined.Contains("World")}");
        Console.WriteLine($"Replace: {combined.Replace("World", "C#")}");

        // 字符串插值
        int age = 30;
        string interpolated = $"年龄: {age}, 明年: {age + 1}";
        Console.WriteLine(interpolated);

        // StringBuilder（高效字符串拼接）
        var sb = new System.Text.StringBuilder();
        for (int i = 0; i < 5; i++)
        {
            sb.Append(i).Append(" ");
        }
        Console.WriteLine($"StringBuilder: {sb}");

        // ── 泛型 ──
        Box<string> box = new Box<string>();
        box.Put("Hello Generics");
        Console.WriteLine(box.Get());
        Console.WriteLine($"Max(3,7): {Utils.Max(3, 7)}");
        Console.WriteLine($"First item: {Utils.First(list)}");

        // ── 异常处理 ──
        try
        {
            int result = Divide(10, 0);
            Console.WriteLine(result);
        }
        catch (DivideByZeroException ex)
        {
            Console.WriteLine($"除零异常: {ex.Message}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"其他异常: {ex.Message}");
        }
        finally
        {
            Console.WriteLine("finally 总是执行");
        }

        // using 语句（自动释放 IDisposable 资源）
        try
        {
            using (var reader = new StringReader("test content"))
            {
                string content = reader.ReadToEnd();
                Console.WriteLine($"StringReader: {content}");
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"IO异常: {ex.Message}");
        }

        // ── 文件操作示例（注释掉以避免实际写入） ──
        // string docPath = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
        // string filePath = Path.Combine(docPath, "test.txt");
        // File.WriteAllText(filePath, "Hello from C#");
        // string readContent = File.ReadAllText(filePath);
        // Console.WriteLine($"File content: {readContent}");

        // 目录操作示例
        Console.WriteLine($"当前目录: {Directory.GetCurrentDirectory()}");
        Console.WriteLine($"Path分隔符: {Path.DirectorySeparatorChar}");

        // ── 委托与事件 ──
        Notifier notifier = new Notifier();
        notifier.OnMessage += msg => Console.WriteLine($"收到消息: {msg}");
        notifier.OnMessage += msg => Console.WriteLine($"日志: {DateTime.Now} - {msg}");
        notifier.SendMessage("Hello Events!");

        // Action / Func 委托
        Action<string> printAction = msg => Console.WriteLine($"Action: {msg}");
        printAction("Hello Action");

        Func<int, int, int> addFunc = (a, b) => a + b;
        Console.WriteLine($"Func add: {addFunc(3, 4)}");

        Predicate<int> isEven = n => n % 2 == 0;
        Console.WriteLine($"IsEven(4): {isEven(4)}");

        // ── 异步编程（async/await） ──
        Console.WriteLine("开始异步操作...");
        string asyncResult = await FetchDataAsync("https://api.example.com");
        Console.WriteLine($"异步结果: {asyncResult}");

        // Task.WhenAll：并行等待多个任务
        var tasks = new List<Task<string>>
        {
            FetchDataAsync("url1"),
            FetchDataAsync("url2"),
            FetchDataAsync("url3")
        };
        string[] allResults = await Task.WhenAll(tasks);
        Console.WriteLine($"并行任务完成: {allResults.Length} 个");

        Console.WriteLine("=== C# 演示完成 ===");
    }

    // 普通方法
    static int Divide(int a, int b)
    {
        if (b == 0)
            throw new DivideByZeroException("除数不能为零");
        return a / b;
    }

    // 异步方法
    static async Task<string> FetchDataAsync(string url)
    {
        // 模拟网络延迟
        await Task.Delay(100);
        return $"Data from {url}";
    }
}
