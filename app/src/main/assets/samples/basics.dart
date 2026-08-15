// Dart：Flutter框架的基础语言，用于跨平台移动应用开发。
// 本示例覆盖：变量、函数、类、继承、Mixin、异步、
//   Stream、泛型、null safety、扩展方法、枚举。

import 'dart:async';
import 'dart:math';

// ── 1. 枚举 ───────────────────────────────────────────
enum Status { pending, active, inactive, deleted }

// ── 2. 抽象类 ────────────────────────────────────────
abstract class Shape {
  double get area;
  void draw();                      // 抽象方法（无实现）
}

// ── 3. 实现接口（用 implements） ──────────────────────
class Circle implements Shape {
  final double radius;
  Circle(this.radius);

  @override
  double get area => pi * radius * radius;

  @override
  void draw() => print('Drawing circle, area=$area');
}

// ── 4. Mixin ──────────────────────────────────────────
mixin Logging {
  void log(String message) {
    print('[${DateTime.now()}] $message');
  }
}

mixin Timestamp {
  DateTime get createdAt => DateTime.now();
}

// 使用 with 关键字混入多个 mixin
class User with Logging, Timestamp {
  final String name;
  final int age;

  // 普通构造函数
  User(this.name, this.age);

  // 命名构造函数
  User.guest() : name = 'Guest', age = 0;

  // factory 构造函数：可以返回缓存实例、子类实例等
  static final Map<String, User> _cache = {};
  factory User.fromCache(String key) {
    return _cache.putIfAbsent(key, () => User(key, 0));
  }

  // const 构造函数：编译期常量
  const User.admin() : name = 'Admin', age = 99;

  void greet() {
    log('User greeted: $name');      // mixin 提供的方法
    print('Hello, $name!');
  }
}

// ── 5. 异步：Future ──────────────────────────────────
Future<String> fetchData({bool simulateError = false}) async {
  await Future.delayed(Duration(seconds: 1));
  if (simulateError) {
    throw Exception('网络请求失败');
  }
  return 'Fetched data';
}

// ── 6. Stream：异步数据流 ────────────────────────────
Stream<int> countStream(int max) async* {
  for (int i = 1; i <= max; i++) {
    await Future.delayed(Duration(milliseconds: 200));
    yield i;                         // 发射值
  }
}

// ── 7. 泛型 ──────────────────────────────────────────
T firstElement<T>(List<T> list) {
  return list.first;
}

class Pair<K, V> {
  final K key;
  final V value;
  const Pair(this.key, this.value);
}

// ── 8. 扩展方法 ──────────────────────────────────────
extension StringExtension on String {
  String hello() => 'Hello, $this!';
  String get capitalized => isEmpty ? '' : '${this[0].toUpperCase()}${substring(1)}';
}

extension IntExtension on int {
  bool get isEven => this % 2 == 0;
  int get squared => this * this;
}

// ── main ──────────────────────────────────────────────
void main() async {
  // ── 变量声明 ──
  // var：类型推断
  var name = 'Alice';              // 推断为 String
  // final：运行时常量
  final age = 30;
  // const：编译时常量
  const PI = 3.14159;
  // late：延迟初始化，首次访问时才赋值
  late String lazyInit;

  // ── null safety ──
  String? nullable = null;         // ? 表示可为 null
  String nonNullable = 'Hello';

  // 安全调用 ?. 和 ?? 空值合并
  print(nullable?.length ?? 0);    // null 时返回 0

  // ! 强制解包（确定非 null 时使用）
  print(nonNullable.length);

  // ?[] ?. 安全索引
  List<int>? nullableList;
  print(nullableList?[0] ?? -1);   // null 时返回 -1

  // ── 集合 ──
  // List
  var nums = [1, 2, 3, 4, 5];
  nums.add(6);
  // Set
  var uniqueNums = {1, 2, 3, 1};   // 重复的 1 被忽略
  print('Set: $uniqueNums');       // {1, 2, 3}
  // Map
  var scores = {'Alice': 95, 'Bob': 87};
  scores['Charlie'] = 92;

  // 集合操作
  var evens = nums.where((n) => n.isEven).toList();
  var squared = nums.map((n) => n * n).toList();
  print('Evens: $evens, Squared: $squared');

  // ── 条件 ──
  int score = 85;
  if (score >= 90) {
    print('A');
  } else if (score >= 80) {
    print('B');
  } else {
    print('C');
  }

  // switch（Dart 3 支持模式匹配）
  switch (score) {
    case >= 90: print('优秀'); break;
    case >= 60: print('及格'); break;
    default:    print('不及格');
  }

  // ── 循环 ──
  for (var i = 0; i < 3; i++) {
    print('for: $i');
  }

  for (final n in nums) {
    print('for-in: $n');
  }

  int w = 3;
  while (w > 0) {
    print('while: $w');
    w--;
  }

  // ── 类与 Mixin ──
  var user = User('Alice', 30);
  user.greet();
  print('Created at: ${user.createdAt}'); // Timestamp mixin 提供

  var guest = User.guest();
  print('Guest: ${guest.name}');

  // ── 抽象类 / 接口 ──
  var circle = Circle(5.0);
  circle.draw();
  print('Circle area: ${circle.area.toStringAsFixed(2)}');

  // ── 异步 ──
  // then().catchError() 模式
  fetchData().then((value) {
    print(value);
  }).catchError((e) {
    print('Error: $e');
  });

  // async/await
  try {
    var result = await fetchData();
    print('Await result: $result');
  } catch (e) {
    print('Caught: $e');
  }

  // Future.wait: 并行等待多个 Future
  var results = await Future.wait([
    fetchData(),
    fetchData(),
  ]);
  print('Wait results: $results');

  // ── Stream ──
  await for (var count in countStream(5)) {
    print('Stream count: $count');
  }

  // Stream 监听
  countStream(3).listen(
    (data) => print('Listen: $data'),
    onDone: () => print('Stream done'),
  );

  // ── 扩展方法 ──
  print('dart'.hello());
  print('hello world'.capitalized);
  print('5.isEven: ${5.isEven}, 5.squared: ${5.squared}');

  // ── 泛型 ──
  var pair = Pair('key', 42);
  print('Pair: ${pair.key} -> ${pair.value}');
  print('First element: ${firstElement<String>(['a', 'b', 'c'])}');

  // ── 枚举 ──
  var status = Status.active;
  print('Status: $status, index: ${status.index}');
}
