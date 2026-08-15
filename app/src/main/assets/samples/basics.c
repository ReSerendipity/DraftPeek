// C语言：底层系统编程语言，操作系统和嵌入式开发的基础。
// 本示例覆盖：头文件、宏、变量、运算符、条件、循环、函数、
//   指针、数组、结构体、联合体、枚举、动态内存、函数指针、
//   文件读写、预处理指令。

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

// ── 1. 宏定义 ─────────────────────────────────────────
// #define 定义常量宏，编译期直接文本替换
#define PI 3.14159
// 带参数的宏，注意用小括号保护参数
#define SQUARE(x) ((x) * (x))
#define MAX(a, b) ((a) > (b) ? (a) : (b))

// ── 2. 枚举 ───────────────────────────────────────────
// enum 用于定义一组命名整数常量
typedef enum {
    COLOR_RED,
    COLOR_GREEN = 10,
    COLOR_BLUE
} Color;

// ── 3. 结构体 ─────────────────────────────────────────
// struct 用于将不同类型的数据组合在一起
typedef struct {
    char name[50];
    int age;
    float score;
} Student;

// 嵌套结构体
typedef struct {
    double x;
    double y;
} Point;

typedef struct {
    Point start;
    Point end;
} Line;

// ── 4. 联合体 ─────────────────────────────────────────
// union 所有成员共享同一块内存，大小等于最大成员
typedef union {
    int intVal;
    float floatVal;
    char strVal[20];
} DataValue;

// ── 5. 函数声明 ───────────────────────────────────────
int add(int a, int b);
void increment(int *ptr);
void print_student(const Student *s);
double average(int count, ...);

// ── 6. 函数定义 ───────────────────────────────────────

// 基本函数：两数相加
int add(int a, int b) {
    return a + b;
}

// 指针参数：通过指针修改外部变量
void increment(int *ptr) {
    if (ptr != NULL) {
        (*ptr)++;
    }
}

// 结构体指针：打印学生信息
void print_student(const Student *s) {
    printf("学生: %s, 年龄: %d, 分数: %.1f\n",
           s->name, s->age, s->score);
}

// 可变参数函数：计算平均值
double average(int count, ...) {
    va_list args;
    va_start(args, count);
    double sum = 0.0;
    for (int i = 0; i < count; i++) {
        sum += va_arg(args, double);
    }
    va_end(args);
    return count > 0 ? sum / count : 0.0;
}

// ── 7. 函数指针 ───────────────────────────────────────
// 函数指针可以实现策略模式和回调机制
typedef int (*Operation)(int, int);

int multiply(int a, int b) { return a * b; }
int subtract(int a, int b) { return a - b; }

// ── 8. 主函数 ─────────────────────────────────────────
int main(void) {
    printf("=== C语言基础示例 ===\n\n");

    // --- 变量与运算 ---
    int x = 10, y = 3;
    printf("整数运算: %d + %d = %d\n", x, y, add(x, y));
    printf("宏运算: SQUARE(%d) = %d\n", x, SQUARE(x));
    printf("宏运算: MAX(%d, %d) = %d\n", x, y, MAX(x, y));

    // --- 枚举 ---
    printf("\n枚举值: RED=%d, GREEN=%d, BLUE=%d\n",
           COLOR_RED, COLOR_GREEN, COLOR_BLUE);

    // --- 结构体 ---
    Student stu = {"张三", 20, 92.5f};
    print_student(&stu);

    Point p1 = {0.0, 0.0};
    Point p2 = {3.0, 4.0};
    Line line = {p1, p2};
    printf("线段: (%.0f,%.0f) -> (%.0f,%.0f)\n",
           line.start.x, line.start.y, line.end.x, line.end.y);

    // --- 联合体 ---
    DataValue val;
    val.intVal = 42;
    printf("\n联合体 int 值: %d\n", val.intVal);
    val.floatVal = 3.14f;
    printf("联合体 float 值: %.2f (int 值被覆盖)\n", val.floatVal);

    // --- 指针 ---
    int num = 100;
    printf("\n指针: 修改前 num = %d\n", num);
    increment(&num);
    printf("指针: 修改后 num = %d\n", num);

    // --- 数组 ---
    int arr[] = {5, 2, 8, 1, 9, 3};
    int n = sizeof(arr) / sizeof(arr[0]);
    // 冒泡排序
    for (int i = 0; i < n - 1; i++) {
        for (int j = 0; j < n - i - 1; j++) {
            if (arr[j] > arr[j + 1]) {
                int tmp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = tmp;
            }
        }
    }
    printf("\n排序后: ");
    for (int i = 0; i < n; i++) printf("%d ", arr[i]);
    printf("\n");

    // --- 动态内存分配 ---
    int *dynamic_arr = (int *)malloc(n * sizeof(int));
    if (dynamic_arr != NULL) {
        for (int i = 0; i < n; i++) dynamic_arr[i] = arr[i] * 2;
        printf("动态数组: ");
        for (int i = 0; i < n; i++) printf("%d ", dynamic_arr[i]);
        printf("\n");
        free(dynamic_arr);  // 释放内存
    }

    // --- 函数指针 ---
    Operation ops[] = {add, subtract, multiply};
    const char *names[] = {"加", "减", "乘"};
    printf("\n函数指针: 10 %s 3 = ", names[0]);
    printf("%d\n", ops[0](10, 3));
    printf("函数指针: 10 %s 3 = ", names[1]);
    printf("%d\n", ops[1](10, 3));
    printf("函数指针: 10 %s 3 = ", names[2]);
    printf("%d\n", ops[2](10, 3));

    // --- 可变参数 ---
    printf("\n可变参数平均值: %.2f\n", average(4, 1.0, 2.0, 3.0, 4.0));

    // --- 字符串操作 ---
    char str1[64] = "Hello";
    char str2[] = ", World!";
    strcat(str1, str2);
    printf("\n字符串拼接: %s\n", str1);
    printf("字符串长度: %lu\n", (unsigned long)strlen(str1));

    // --- 条件与循环 ---
    printf("\nFizzBuzz (1-15):\n  ");
    for (int i = 1; i <= 15; i++) {
        if (i % 15 == 0)      printf("FizzBuzz ");
        else if (i % 3 == 0)  printf("Fizz ");
        else if (i % 5 == 0)  printf("Buzz ");
        else                  printf("%d ", i);
    }
    printf("\n");

    printf("\n=== 示例结束 ===\n");
    return 0;
}
