// JavaScript：Web前端的核心语言，支持函数式、面向对象和异步编程。
// 本示例覆盖：变量、数据类型、函数、闭包、原型、类、Promise、
//   async/await、模块、Map/Set、Symbol、生成器、Proxy、Reflect。

// ── 1. 变量声明：var / let / const ────────────────────
var globalVar = "全局作用域";         // var：函数作用域
let blockVar = "块作用域";            // let：块作用域
const PI = 3.14159;                  // const：常量，不可重新赋值

// ── 2. 数据类型 ───────────────────────────────────────
const str = "Hello";                 // 字符串
const num = 42;                      // 数字（不区分整数/浮点）
const bool = true;                   // 布尔
const nothing = null;                // 空值
const notDefined = undefined;        // 未定义
const bigInt = 9007199254740991n;    // BigInt：大整数
const sym = Symbol("唯一标识");       // Symbol：唯一值

// ── 3. 模板字符串 ─────────────────────────────────────
const firstName = "Alice";
const age = 30;
const greeting = `Hello, ${firstName}! You are ${age} years old.`;
// 多行模板字符串
const multiline = `
  第一行
  第二行: ${2 + 3}
`;

// ── 4. 解构赋值 ───────────────────────────────────────
const [first, second, ...rest] = [1, 2, 3, 4, 5];     // 数组解构
const { name, age: userAge = 0 } = { name: "Bob" };     // 对象解构 + 默认值

// ── 5. 函数：多种写法 ─────────────────────────────────
// 普通函数声明
function add(a, b) {
    return a + b;
}

// 函数表达式
const subtract = function(a, b) {
    return a - b;
};

// 箭头函数（不绑定 this）
const multiply = (a, b) => a * b;
const square = x => x * x;

// 默认参数
function greet(name = "世界") {
    return `你好, ${name}!`;
}

// 剩余参数（...rest）
function sumAll(...numbers) {
    return numbers.reduce((acc, n) => acc + n, 0);
}

// ── 6. 闭包（Closure） ────────────────────────────────
// 函数携带其定义时的外部作用域变量
function createCounter(initial = 0) {
    let count = initial;
    return {
        increment: () => ++count,
        decrement: () => --count,
        getValue: () => count,
    };
}

// ── 7. 高阶函数：filter / map / reduce ─────────────────
const numbers = [1, 2, 3, 4, 5];
const evens = numbers.filter(n => n % 2 === 0);
const doubled = numbers.map(n => n * 2);
const sum = numbers.reduce((acc, n) => acc + n, 0);

// 链式调用
const result = numbers
    .filter(n => n > 2)
    .map(n => n * 10)
    .reduce((a, b) => a + b, 0);

// ── 8. 类（ES6+） ─────────────────────────────────────
class Animal {
    constructor(name) {
        this.name = name;
    }

    // 实例方法
    speak() {
        return `${this.name} makes a sound.`;
    }

    // getter
    get displayName() {
        return `【${this.name}】`;
    }

    // setter
    set displayName(value) {
        this.name = value;
    }

    // 静态方法
    static kingdom() {
        return "Animalia";
    }
}

// 存在类前，用原型继承
function OldAnimal(name) {
    this.name = name;
}
OldAnimal.prototype.speak = function() {
    return `${this.name} makes a sound.`;
};

// 继承
class Dog extends Animal {
    constructor(name, breed) {
        super(name);                 // 调用父类构造器
        this.breed = breed;
    }

    speak() {
        return `${this.name} barks! (breed: ${this.breed})`;
    }
}

// ── 9. Promise ────────────────────────────────────────
function fetchData(url) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            if (url) {
                resolve(`Data from ${url}`);
            } else {
                reject(new Error("URL required"));
            }
        }, 1000);
    });
}

// then/catch/finally 链式调用
fetchData("/api")
    .then(data => {
        console.log(data);
        return data.toUpperCase();
    })
    .catch(error => console.error("Error:", error.message))
    .finally(() => console.log("请求结束"));

// Promise 组合
Promise.all([fetchData("/a"), fetchData("/b")]).then(results => {
    console.log("All results:", results);
});

Promise.race([fetchData("/fast"), fetchData("/slow")]).then(winner => {
    console.log("Winner:", winner);
});

// ── 10. async/await ──────────────────────────────────
async function main() {
    try {
        const data = await fetchData("/async");
        console.log("Async:", data);
    } catch (error) {
        console.error("Async error:", error);
    }
}

// ── 11. Map / Set / WeakMap / WeakSet ──────────────────
const map = new Map();
map.set("key", "value");
map.set({ id: 1 }, "object key");    // 对象也可以做键
console.log("Map size:", map.size);

const set = new Set([1, 2, 3, 2, 1]); // 自动去重
console.log("Set:", [...set]);

// WeakMap：键必须是对象，不阻止垃圾回收
const wm = new WeakMap();
const obj = {};
wm.set(obj, "private data");

// ── 12. 生成器（Generator） ──────────────────────────
function* fibonacci(n) {
    let [a, b] = [0, 1];
    for (let i = 0; i < n; i++) {
        yield a;
        [a, b] = [b, a + b];
    }
}

// ── 13. Proxy 与 Reflect ──────────────────────────────
const handler = {
    get(target, prop) {
        console.log(`读取属性: ${String(prop)}`);
        return Reflect.get(target, prop);
    },
    set(target, prop, value) {
        console.log(`设置 ${String(prop)} = ${value}`);
        return Reflect.set(target, prop, value);
    },
};
const proxyObj = new Proxy({ x: 10, y: 20 }, handler);

// ── 14. this 绑定 ─────────────────────────────────────
const obj2 = {
    value: 42,
    getValue() { return this.value; },                    // 方法中的 this = obj2
    getValueArrow: () => { return this.value; },          // 箭头函数不绑定 this
};

// ── 15. 可选链 ?. 与空值合并 ?? ────────────────────────
const deep = { a: { b: { c: 42 } } };
console.log(deep?.a?.b?.c ?? "not found"); // 42

// ── 16. 展开运算符 ────────────────────────────────────
const arr1 = [1, 2];
const arr2 = [3, 4];
const combined = [...arr1, ...arr2];    // [1, 2, 3, 4]
const obj1 = { a: 1 };
const obj2a = { b: 2 };
const merged = { ...obj1, ...obj2a };   // { a: 1, b: 2 }

// ── 17. 逻辑短路赋值 ──────────────────────────────────
const user = null;
const display = user?.name ?? "匿名";    // 可选链 + 空值合并

// ── 18. 模块导出（若使用 ES Modules） ──────────────────
// export { add, multiply, fetchData, main };
// export default Dog;

// ── 执行 ──────────────────────────────────────────────
console.log(add(5, 3));
console.log(square(6));
console.log(greeting);

const counter = createCounter();
counter.increment();
counter.increment();
console.log("Counter:", counter.getValue());

console.log("Evens:", evens);
console.log("Sum:", sum);

const dog = new Dog("Rex", "German Shepherd");
console.log(dog.speak());
console.log(dog.displayName);          // getter
console.log(Animal.kingdom());         // 静态方法

for (const n of fibonacci(10)) {
    console.log(`fib: ${n}`);
}

proxyObj.x = 100;                      // 触发 Proxy set
console.log(proxyObj.x);               // 触发 Proxy get

console.log("Generator:", [...fibonacci(5)]);
console.log("SumAll:", sumAll(1, 2, 3, 4, 5));
console.log("Combined:", combined);
console.log("Optional chaining:", display);

main();
