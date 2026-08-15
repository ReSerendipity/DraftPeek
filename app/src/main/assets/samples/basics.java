// Java：JVM生态的主力语言，强调面向对象、跨平台。
// 本示例覆盖：类、抽象类、接口、内部类、枚举、泛型、
//   集合框架、Stream API、异常、try-with-resources、多线程、注解。

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

// ── 1. 接口（interface） ──────────────────────────────
// Java 8+ 接口可以有 default 方法和 static 方法
interface Drawable {
    void draw();                     // 抽象方法

    // default 方法：子类自动继承，可覆盖
    default void log() {
        System.out.println("Drawing...");
    }
}

// ── 2. 抽象类（abstract class） ───────────────────────
abstract class Shape {
    String name;
    Shape(String name) { this.name = name; }

    // 抽象方法：子类必须实现
    abstract double area();

    // 具体方法
    void printInfo() {
        System.out.println(name + " area=" + area());
    }
}

// ── 3. 类与继承 ───────────────────────────────────────
class Circle extends Shape implements Drawable {
    private double radius;          // 封装：private 字段
    static int circleCount = 0;     // 类变量（所有实例共享）

    // 构造器
    Circle(String name, double radius) {
        super(name);                // 调用父类构造器
        this.radius = radius;
        circleCount++;
    }

    // 方法重写
    @Override
    double area() { return Math.PI * radius * radius; }

    @Override
    public void draw() {
        System.out.println("Drawing circle with radius " + radius);
    }
}

// ── 4. 内部类与匿名类 ────────────────────────────────
class OuterClass {
    private int outerField = 10;

    // 成员内部类
    class InnerClass {
        void show() {
            System.out.println("Outer field = " + outerField);
        }
    }

    // 静态嵌套类
    static class StaticNested {
        void say() { System.out.println("Static nested"); }
    }
}

// ── 5. 枚举 ───────────────────────────────────────────
enum Color {
    RED("#FF0000"),
    GREEN("#00FF00"),
    BLUE("#0000FF");

    private final String hex;
    Color(String hex) { this.hex = hex; }
    String getHex() { return hex; }
}

// ── 6. 泛型类 ─────────────────────────────────────────
class Box<T> {
    private T item;
    void put(T item) { this.item = item; }
    T get() { return item; }
}

// ── 7. 泛型方法 ───────────────────────────────────────
class Utils {
    static <T> T first(List<T> list) { return list.get(0); }
    static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) > 0 ? a : b;
    }
}

// ── main ──────────────────────────────────────────────
public class Basics {
    public static void main(String[] args) {
        // ── 变量与常量 ──
        int count = 10;              // 基本类型
        final double PI = 3.14159;   // final = 不可修改常量
        char letter = 'A';
        boolean flag = true;

        // ── 条件 ──
        if (count > 5) {
            System.out.println("大于5");
        } else if (count > 0) {
            System.out.println("1-5之间");
        } else {
            System.out.println("非正数");
        }

        // switch（支持字符串、枚举、int；Java 14+ 支持 -> 语法）
        String day = "Mon";
        switch (day) {
            case "Mon" -> System.out.println("周一");
            case "Fri" -> System.out.println("周五");
            default   -> System.out.println("其他");
        }

        // ── 循环 ──
        for (int i = 0; i < 3; i++) {
            System.out.print(i + " ");
        }
        System.out.println();

        int[] arr = {1, 2, 3, 4, 5};
        // 增强 for
        for (int n : arr) {
            System.out.print(n + " ");
        }
        System.out.println();

        int w = 0;
        while (w < 3) {
            System.out.print("w");
            w++;
        }
        System.out.println();

        // ── 数组 ──
        String[] names = {"Alice", "Bob", "Charlie"};

        // ── 多态 ──
        Shape circle = new Circle("MyCircle", 5.0);
        circle.printInfo();
        ((Drawable) circle).draw();
        System.out.println("Circles created: " + Circle.circleCount);

        // ── 枚举 ──
        Color color = Color.RED;
        System.out.println("Color: " + color + " hex=" + color.getHex());

        // ── 集合框架 ──
        // List（有序可重复）
        List<String> list = new ArrayList<>();
        list.add("A"); list.add("B"); list.add("C");

        // Set（无序不可重复）
        Set<Integer> set = new HashSet<>();
        set.add(1); set.add(2); set.add(1); // 重复的 1 被忽略
        System.out.println("Set size: " + set.size());

        // Map（键值对）
        Map<String, Integer> map = new HashMap<>();
        map.put("Alice", 95);
        map.put("Bob", 87);
        map.forEach((k, v) -> System.out.println(k + ": " + v));

        // Queue
        Queue<String> queue = new LinkedList<>();
        queue.offer("first");
        queue.offer("second");
        System.out.println("Queue peek: " + queue.peek());

        // ── Stream API ──
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> evens = numbers.stream()
            .filter(n -> n % 2 == 0)    // 过滤
            .map(n -> n * n)            // 映射
            .sorted(Comparator.reverseOrder()) // 排序
            .collect(Collectors.toList());
        System.out.println("Evens squared: " + evens);

        // reduce 归约
        int sum = numbers.stream().reduce(0, Integer::sum);
        System.out.println("Sum: " + sum);

        // ── 泛型 ──
        Box<String> box = new Box<>();
        box.put("Hello Generics");
        System.out.println(box.get());
        System.out.println("Max(3,7): " + Utils.max(3, 7));

        // ── 异常处理 ──
        try {
            int r = divide(10, 0);
            System.out.println(r);
        } catch (ArithmeticException e) {
            System.out.println("算术异常: " + e.getMessage());
        } finally {
            System.out.println("finally 总是执行");
        }

        // try-with-resources（自动关闭 AutoCloseable 资源）
        try (Scanner scanner = new Scanner("Hello Java")) {
            System.out.println("Scanner: " + scanner.next());
        } // 自动调用 close()

        // ── 内部类 ──
        OuterClass outer = new OuterClass();
        OuterClass.InnerClass inner = outer.new InnerClass();
        inner.show();
        new OuterClass.StaticNested().say();

        // ── 多线程 ──
        // 方式1：继承 Thread
        Thread t1 = new Thread(() -> System.out.println("Thread running"));
        t1.start();

        // 方式2：ExecutorService 线程池
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Integer> future = executor.submit(() -> {
            Thread.sleep(100);
            return 42;
        });
        try {
            System.out.println("Future result: " + future.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }

    static int divide(int a, int b) {
        if (b == 0) throw new ArithmeticException("除数不能为零");
        return a / b;
    }
}
