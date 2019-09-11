package com.pharbers

/** 功能描述
  *
  * @param args 构造参数
  * @tparam T 构造泛型参数
  * @author dcs
  * @version 0.0
  * @since 2019/09/11 15:07
  * @note 一些值得注意的地方
  */
object test extends App{
    def curry[A, B, C](f: (A, B) => C): A => B => C =
        a => b => f(a, b)
    def f(a: Int, b: Int): Int = a + b
    def g(a: Int)(b: Int): Int = a + b
    println(g(1)(1))
}
