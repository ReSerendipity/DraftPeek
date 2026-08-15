# Python：简洁易学的高级编程语言，广泛用于Web开发、数据分析和AI。
# 本示例覆盖：变量、数据类型、条件、循环、函数、类、继承、
#   装饰器、生成器、上下文管理器、异常、asyncio、match-case。

from typing import List, Optional, Protocol, Generator
from dataclasses import dataclass, field
from functools import wraps
from collections import defaultdict, Counter, namedtuple
import asyncio

# ── 1. 装饰器：包装函数，不改原函数代码 ─────────────────
def log_call(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        print(f"[LOG] 调用 {func.__name__}({args}, {kwargs})")
        return func(*args, **kwargs)
    return wrapper

# 带参数的装饰器
def repeat(n: int):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            for _ in range(n):
                func(*args, **kwargs)
        return wrapper
    return decorator

# ── 2. 类与继承 ────────────────────────────────────────
class Animal:
    # 类变量（所有实例共享）
    species_count = 0

    def __init__(self, name: str):
        self.name = name             # 实例变量
        Animal.species_count += 1

    # 实例方法
    def speak(self) -> str:
        return f"{self.name} 发出声音"

    # 静态方法：不依赖实例，用 @staticmethod
    @staticmethod
    def is_animal(name: str) -> bool:
        return bool(name)

    # 类方法：接收类本身，用 @classmethod
    @classmethod
    def get_count(cls) -> int:
        return cls.species_count

    # 属性 getter/setter
    @property
    def display_name(self) -> str:
        return self.name.upper()

# 继承
class Dog(Animal):
    def __init__(self, name: str, breed: str):
        super().__init__(name)       # 调用父类初始化
        self.breed = breed

    # 方法重写
    def speak(self) -> str:
        return f"{self.name} (品种: {self.breed}) 说：汪！"

# 多继承
class Flyable:
    def fly(self) -> str:
        return "正在飞行"

class Bat(Animal, Flyable):
    def speak(self) -> str:
        return f"{self.name}：吱吱"

# ── 3. 数据类（dataclass） ─────────────────────────────
@dataclass
class User:
    name: str
    age: int
    email: Optional[str] = None     # Optional = 可为 None
    tags: List[str] = field(default_factory=list)  # 可变默认值必须用 field

# ── 4. 生成器（yield） ────────────────────────────────
def fibonacci(n: int) -> Generator[int, None, None]:
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b

# 生成器表达式（惰性求值）
squares_gen = (x * x for x in range(1000000))  # 不立即计算

# ── 5. 上下文管理器 ────────────────────────────────────
class FileManager:
    """自定义上下文管理器：__enter__ + __exit__"""
    def __init__(self, filename: str):
        self.filename = filename

    def __enter__(self):
        self.file = open(self.filename, "w")
        return self.file

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.file.close()
        print(f"文件 {self.filename} 已关闭")

# ── 6. 协程（asyncio） ─────────────────────────────────
async def fetch_data(url: str, delay: float = 1.0) -> str:
    print(f"开始请求 {url}")
    await asyncio.sleep(delay)       # 模拟网络延迟
    return f"来自 {url} 的数据"

async def main_async():
    # 并发执行多个协程（asyncio.gather）
    results = await asyncio.gather(
        fetch_data("https://api1.example.com", 1.0),
        fetch_data("https://api2.example.com", 0.5),
        fetch_data("https://api3.example.com", 0.8),
    )
    return results

# ── 7. 协议（Protocol，结构化类型） ─────────────────────
class Drawable(Protocol):
    def draw(self) -> None: ...

class Rectangle:
    def draw(self) -> None:
        print("画一个矩形")

# ── main ──────────────────────────────────────────────
@log_call
def greet(name: str) -> str:
    """演示装饰器与类型标注"""
    return f"Hello, {name}!"

if __name__ == "__main__":
    # ── 变量与数据类型 ──
    x: int = 42
    pi: float = 3.14159
    name: str = "Python"
    is_active: bool = True

    # ── 集合类型 ──
    # list：有序可重复
    fruits: list = ["apple", "banana", "cherry"]
    # tuple：不可变序列
    point: tuple = (3, 4)
    # dict：键值对
    scores: dict = {"Alice": 95, "Bob": 87}
    # set：无序不重复
    unique: set = {1, 2, 3, 1}

    # ── 条件 ──
    score = 85
    if score >= 90:
        print("A")
    elif score >= 80:
        print("B")
    else:
        print("C")

    # 三元表达式 =  val_if_true if condition else val_if_false
    result = "及格" if score >= 60 else "不及格"
    print(result)

    # ── match-case（Python 3.10+） ──
    match score:
        case n if n >= 90:
            print("优秀")
        case n if n >= 60:
            print("通过")
        case _:
            print("不通过")

    # ── 循环 ──
    # for 循环
    for i, fruit in enumerate(fruits):
        print(f"{i}:{fruit} ", end="")
    print()

    # range
    for i in range(5):               # 0,1,2,3,4
        print(i, end=" ")
    print()

    # while
    count = 3
    while count > 0:
        print(f"w{count} ", end="")
        count -= 1
    print()

    # ── 列表推导式 ──
    numbers = [1, 2, 3, 4, 5]
    # 过滤 + 映射一行搞定
    squared_evens = [n**2 for n in numbers if n % 2 == 0]
    print(f"Squared evens: {squared_evens}")

    # 字典推导式
    num_squares = {n: n**2 for n in numbers}
    print(f"Number squares: {num_squares}")

    # 集合推导式
    unique_lengths = {len(fruit) for fruit in fruits}
    print(f"Unique lengths: {unique_lengths}")

    # ── 函数 ──
    print(greet("World"))
    print(f"Fibonacci(10): {list(fibonacci(10))}")

    # ── 异常处理 ──
    try:
        value = int("abc")
    except ValueError as e:
        print(f"转换失败: {e}")
    except (TypeError, KeyError):
        print("其他已知错误")
    else:
        print("没有异常时执行")         # 无异常时
    finally:
        print("总是执行")               # 无论有无异常

    # 抛出异常
    # raise ValueError("自定义错误")

    # ── 上下文管理器（with 语句） ──
    with open("output.txt", "w") as f:
        f.write("Hello from Python\n")
    # 自动关闭文件

    # 自定义上下文管理器
    with FileManager("custom.txt") as f:
        f.write("自定义管理器")

    # ── 类 ──
    dog = Dog("旺财", "金毛")
    print(dog.speak())
    print(f"动物总数: {Animal.get_count()}")

    # ── 数据类 ──
    user = User("Alice", 30, tags=["python", "developer"])
    print(f"User: {user}")
    # 数据类自动生成 __repr__、__eq__ 等

    # ── 嵌套函数与闭包 ──
    def make_multiplier(factor: int):
        def multiplier(x: int) -> int:
            return x * factor
        return multiplier

    double = make_multiplier(2)
    triple = make_multiplier(3)
    print(f"双倍: {double(5)}, 三倍: {triple(5)}")

    # ── lambda 函数 ──
    add = lambda a, b: a + b
    print(f"Lambda add: {add(3, 4)}")

    # ── map / filter / reduce ──
    from functools import reduce
    doubled = list(map(lambda n: n * 2, numbers))
    evens = list(filter(lambda n: n % 2 == 0, numbers))
    total = reduce(lambda a, b: a + b, numbers, 0)
    print(f"map: {doubled}, filter: {evens}, reduce: {total}")

    # ── unpacking 解包 ──
    a, b, *rest = [1, 2, 3, 4, 5]     # * 收集剩余
    print(f"a={a}, b={b}, rest={rest}")
    *prefix, last = (1, 2, 3, 4)       # 前面收集

    # ── defaultdict / Counter ──
    word_counter = Counter("abracadabra")
    print(f"Counter: {word_counter}")
    grouped = defaultdict(list)
    grouped["fruit"].append("apple")
    print(f"defaultdict: {dict(grouped)}")

    # ── zip 并行遍历 ──
    names = ["Alice", "Bob", "Charlie"]
    ages = [25, 30, 35]
    for n, a in zip(names, ages):
        print(f"{n}:{a} ", end="")
    print()

    # ── 多继承 ──
    bat = Bat("小蝙蝠")
    print(bat.speak())
    print(bat.fly())

    # ── 装饰器示例 ──
    @repeat(3)
    def say_hello():
        print("Hello!")

    say_hello()

    # ── asyncio 运行 ──
    results = asyncio.run(main_async())
    for r in results:
        print(f"异步结果: {r}")

    # ── Protocol ──
    rect = Rectangle()
    rect.draw()
    # Python 不会强制类型检查，Protocol 用于静态分析（mypy）
