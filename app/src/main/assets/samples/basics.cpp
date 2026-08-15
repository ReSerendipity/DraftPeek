// C++：C的增强版，支持面向对象、模板、标准库。
// 本示例覆盖：类与对象、继承、多态、运算符重载、模板、
//   拷贝控制、移动语义、Lambda、STL容器/算法、异常、智能指针。

#include <iostream>
#include <vector>
#include <string>
#include <memory>
#include <stdexcept>
#include <algorithm>
#include <map>
#include <unordered_map>
#include <set>
#include <functional>

// ── 1. 类与对象、封装 ─────────────────────────────────
class Animal {
private:
    std::string name_;               // 私有成员
protected:
    int age_ = 0;                    // 受保护成员：子类可访问
public:
    // 构造函数
    explicit Animal(std::string name) : name_(std::move(name)) {}
    // 虚析构函数：确保子类正确析构
    virtual ~Animal() = default;

    // getter
    const std::string& name() const { return name_; }

    // 纯虚函数 = 0：强制子类重写，此类成为抽象类
    virtual void speak() const = 0;

    // 普通虚函数：子类可选择重写
    virtual void info() const {
        std::cout << "Animal: " << name_ << std::endl;
    }
};

// ── 2. 继承 ───────────────────────────────────────────
class Dog : public Animal {          // public 继承，is-a 关系
public:
    using Animal::Animal;            // 继承基类构造函数
    // override 确保正在重写虚函数
    void speak() const override { std::cout << "Woof!" << std::endl; }
};

class Cat : public Animal {
public:
    Cat(std::string name, int lives) : Animal(std::move(name)), lives_(lives) {}
    void speak() const override { std::cout << "Meow!" << std::endl; }
private:
    int lives_;
};

// ── 3. 运算符重载 ─────────────────────────────────────
class Point {
public:
    Point(int x, int y) : x_(x), y_(y) {}
    // 重载 + 号运算符
    Point operator+(const Point& other) const {
        return Point(x_ + other.x_, y_ + other.y_);
    }
    // 重载 << 输出运算符（友元函数，非成员）
    friend std::ostream& operator<<(std::ostream& os, const Point& p) {
        os << "(" << p.x_ << ", " << p.y_ << ")";
        return os;
    }
private:
    int x_, y_;
};

// ── 4. 拷贝控制与移动语义 ─────────────────────────────
class Buffer {
private:
    int* data_;
    size_t size_;
public:
    explicit Buffer(size_t s) : size_(s), data_(new int[s]{}) {}
    // 拷贝构造（深拷贝）
    Buffer(const Buffer& other) : size_(other.size_), data_(new int[other.size_]) {
        std::copy(other.data_, other.data_ + size_, data_);
    }
    // 拷贝赋值
    Buffer& operator=(const Buffer& other) {
        if (this != &other) {
            delete[] data_;
            size_ = other.size_;
            data_ = new int[size_];
            std::copy(other.data_, other.data_ + size_, data_);
        }
        return *this;
    }
    // 移动构造（转移资源所有权，不拷贝）
    Buffer(Buffer&& other) noexcept : size_(other.size_), data_(other.data_) {
        other.data_ = nullptr;
        other.size_ = 0;
    }
    // 移动赋值
    Buffer& operator=(Buffer&& other) noexcept {
        if (this != &other) {
            delete[] data_;
            data_ = other.data_;
            size_ = other.size_;
            other.data_ = nullptr;
            other.size_ = 0;
        }
        return *this;
    }
    ~Buffer() { delete[] data_; }
};

// ── 5. 模板函数与模板类 ───────────────────────────────
template<typename T>
T max_value(T a, T b) { return (a > b) ? a : b; }

// 变参模板（折叠表达式，C++17）
template<typename... Args>
auto sum_all(Args... args) { return (args + ...); }

template<typename T>
class Stack {
    std::vector<T> data_;
public:
    void push(const T& val) { data_.push_back(val); }
    T pop() {
        auto val = data_.back();
        data_.pop_back();
        return val;
    }
    [[nodiscard]] bool empty() const { return data_.empty(); }
};

// ── 6. Lambda 表达式 ──────────────────────────────────
// [捕获列表](参数列表) -> 返回类型 { 函数体 }

// ── main ──────────────────────────────────────────────
int main() {
    // ── 多态 ──
    std::unique_ptr<Animal> dog = std::make_unique<Dog>("Rex");
    std::unique_ptr<Animal> cat = std::make_unique<Cat>("Kitty", 9);
    dog->speak();
    cat->speak();

    // ── 运算符重载 ──
    Point p1(1, 2), p2(3, 4);
    Point p3 = p1 + p2;
    std::cout << "Point: " << p3 << std::endl;

    // ── 模板 ──
    std::cout << "Max: " << max_value(10, 20) << std::endl;
    std::cout << "Sum: " << sum_all(1, 2, 3, 4, 5) << std::endl;

    // ── STL 容器 ──
    // vector：动态数组
    std::vector<int> nums = {5, 2, 8, 1, 9, 3};
    // sort 算法
    std::sort(nums.begin(), nums.end());
    // find 算法
    auto it = std::find(nums.begin(), nums.end(), 5);
    if (it != nums.end()) std::cout << "Found: " << *it << std::endl;
    // 范围 for
    for (const auto& n : nums) std::cout << n << " ";
    std::cout << std::endl;

    // map：有序键值对
    std::map<std::string, int> scores;
    scores["Alice"] = 95;
    scores["Bob"] = 87;
    for (const auto& [name, score] : scores) {
        std::cout << name << ": " << score << std::endl;
    }

    // unordered_map：哈希表（无序）
    std::unordered_map<std::string, int> cache;
    cache["key"] = 42;

    // set：有序不重复集合
    std::set<int> unique_num = {3, 1, 4, 1, 5};  // 重复的1被忽略

    // ── Lambda ──
    int factor = 2;
    // [=] 值捕获所有外部变量；[&] 引用捕获所有
    auto multiply = [factor](int x) { return x * factor; };
    std::cout << "Lambda: 5*2=" << multiply(5) << std::endl;

    // 用 Lambda 作为 sort 的比较器
    std::sort(nums.begin(), nums.end(),
              [](int a, int b) { return a > b; });  // 降序

    // ── 智能指针 ──
    // unique_ptr：独占所有权
    auto up = std::make_unique<std::string>("Hello");
    std::cout << *up << std::endl;
    // shared_ptr：共享所有权，引用计数
    auto sp1 = std::make_shared<int>(100);
    auto sp2 = sp1;  // 引用计数变为2
    std::cout << "shared_ptr count: " << sp1.use_count() << std::endl;

    // ── 异常 ──
    try {
        throw std::runtime_error("演示异常捕获");
    } catch (const std::exception& e) {
        std::cout << "异常: " << e.what() << std::endl;
    }

    // ── constexpr 编译期计算 ──
    constexpr int compile_result = 10 * 20;
    std::cout << "constexpr: " << compile_result << std::endl;

    // ── 范围 for + 结构化绑定 (C++17) ──
    std::map<int, std::string> id_map = {{1, "one"}, {2, "two"}};
    for (const auto& [id, name] : id_map) {
        std::cout << id << "->" << name << " ";
    }
    std::cout << std::endl;

    return 0;
}
