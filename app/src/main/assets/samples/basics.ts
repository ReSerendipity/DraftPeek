// TypeScript：JavaScript的强化版，增加了静态类型系统，适合大型项目。
// 本示例覆盖：基础类型、接口、类型别名、泛型、枚举、
//   联合/交叉/条件类型、keyof、typeof、装饰器、命名空间。

// ── 1. 基础类型 ───────────────────────────────────────
let count: number = 10;
let message: string = "Hello TS";
let isActive: boolean = true;
let nothing: null = null;
let notHere: undefined = undefined;
let bigNum: bigint = 100n;
let unique: symbol = Symbol("id");
let voidVar: void = undefined;       // void：表示无返回值

// unknown：安全版本的 any，使用前必须类型检查
let unknownVal: unknown = 42;
if (typeof unknownVal === "number") {
    let numVal: number = unknownVal; // 类型收窄后才可用
}

// never：永远不会发生的类型（抛异常/无限循环）
function throwError(msg: string): never {
    throw new Error(msg);
}

// ── 2. 数组与元组 ─────────────────────────────────────
const nums: number[] = [1, 2, 3];
const strings: Array<string> = ["a", "b"];  // 泛型写法
// 元组：固定长度、固定类型
const tuple: [string, number, boolean] = ["hello", 42, true];
// 具名元组（可读性更好）
const point: [x: number, y: number] = [10, 20];

// ── 3. 接口（Interface） ──────────────────────────────
interface User {
    readonly id: number;             // 只读，创建后不可修改
    name: string;
    email?: string;                  // 可选属性
    createdAt?: Date;
}

// 接口可扩展
interface Admin extends User {
    role: "admin" | "superadmin";
    permissions: string[];
}

// 索引签名：允许任意字符串属性
interface StringMap {
    [key: string]: string;
}

// ── 4. 类型别名（Type Alias） ─────────────────────────
// 联合类型
type Status = "active" | "inactive" | "pending";
// 交叉类型：合并多个类型
type Colored = { color: string };
type Sized = { width: number; height: number };
type Box = Colored & Sized;

// ── 5. 泛型（Generics） ───────────────────────────────
// 泛型函数
function firstElement<T>(arr: T[]): T | undefined {
    return arr[0];
}

// 泛型约束
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
    return obj[key];
}

// 泛型类
class Stack<T> {
    private items: T[] = [];
    push(item: T): void { this.items.push(item); }
    pop(): T | undefined { return this.items.pop(); }
}

// ── 6. 枚举（Enum） ──────────────────────────────────
enum Direction {
    Up = "UP",                        // 字符串枚举
    Down = "DOWN",
    Left = "LEFT",
    Right = "RIGHT",
}

// 数字枚举（默认 0 开始）
enum HttpCode {
    OK = 200,
    NotFound = 404,
    InternalError = 500,
}

// const enum：编译时内联，不生成对象
const enum Colors {
    Red,
    Green,
    Blue,
}

// ── 7. keyof / typeof ──────────────────────────────────
const person = { name: "Alice", age: 30 };
type Person = typeof person;          // 从值推断类型
type PersonKeys = keyof Person;       // "name" | "age"

// ── 8. 条件类型 ───────────────────────────────────────
type IsString<T> = T extends string ? true : false;
type Test1 = IsString<"hello">;       // true
type Test2 = IsString<42>;            // false

// ── 9. 映射类型 ───────────────────────────────────────
type Readonly<T> = {
    readonly [K in keyof T]: T[K];    // 所有属性转为只读
};
type Partial<T> = {
    [K in keyof T]?: T[K];            // 所有属性可选（内置类型）
};
type ReadonlyUser = Readonly<User>;

// ── 10. 装饰器（需开启 experimentalDecorators） ────────
function Log(target: any, key: string, descriptor: PropertyDescriptor) {
    const original = descriptor.value;
    descriptor.value = function (...args: any[]) {
        console.log(`调用 ${key}(${args})`);
        return original.apply(this, args);
    };
}

class Service {
    @Log
    calculate(a: number, b: number): number {
        return a + b;
    }
}

// ── 11. 异步 Promise 类型 ─────────────────────────────
async function fetchUser(id: number): Promise<User> {
    const response = await fetch(`/api/users/${id}`);
    const data: unknown = await response.json();
    return data as User;              // 类型断言
}

// ── 12. 命名空间 ──────────────────────────────────────
namespace Utils {
    export function add(a: number, b: number): number {
        return a + b;
    }
}

// ── 13. 类型守卫 ──────────────────────────────────────
function isString(value: unknown): value is string {
    return typeof value === "string";
}

// ── 14. Record / Pick / Omit ──────────────────────────
type UserRecord = Record<string, User>;  // { [key: string]: User }
type UserBrief = Pick<User, "id" | "name">;  // 选取部分
type UserNoId = Omit<User, "id">;             // 排除部分

// ── 执行示例 ──────────────────────────────────────────
const user: User = {
    id: 1,
    name: "Alice",
    email: "alice@example.com",
    createdAt: new Date(),
};

const admin: Admin = {
    id: 2,
    name: "Bob",
    role: "admin",
    permissions: ["read", "write"],
};

const status: Status = "active";
const box: Box = { color: "red", width: 100, height: 200 };

const stack = new Stack<string>();
stack.push("hello");
console.log("Stack top:", stack.pop());

const directions: Direction[] = [Direction.Up, Direction.Down];
console.log("Directions:", directions);

// keyof 示例
console.log(getProperty(user, "name"));

// 类型断言
const canvas = document.getElementById("canvas") as HTMLCanvasElement;
// 非空断言 !
const el = document.getElementById("app")!;

// 可选链 + 空值合并
console.log(user?.email?.toLowerCase() ?? "no email");

console.log(firstElement([1, 2, 3]));
console.log(Utils.add(1, 2));

const svc = new Service();
svc.calculate(1, 2);

// 条件类型
const isStringResult: IsString<"hello"> = true;
console.log("IsString:", isStringResult);
