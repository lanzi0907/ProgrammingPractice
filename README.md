# 说一说Go语言并发模型中的Channel
Go语言的并发模型以其简练而独特的设计，对开发者的简洁易用，高性能而区分于其他的传统开发语言如C++，Java等，也是Go语言的一大特色。Go语言提供了围绕着Goroutine，Channel为核心概念的并发特性，正确地理解和使用它们，不仅可以避免程序出现一些并发或同步方面难以预知的运行问题，也有助于开发者更优雅地设计出利用Go并发模式的模块及系统。<br>
本文主要针对Go并发模型下的Channel的使用，结合实际开发中的体会做一下总结，希望借此帮助接触Go语言不久的开发者们正确掌握Channel这一重要并发特性的打开方式，并在实际项目中自如地使用Go语言的并发特性，开发出正确而高效的程序。
## Channel概述
开始学习Go的同学们一定都知道Channel了，这可以说是Go语言提供最亮眼的一个语言特性了。顾名思义，channel即通道，是Go语言中用来在不同Goroutine之间进行通讯的管道。Go语言中的各种类型，从原生类型int, int64, string, map等，到struct，interface，pointer，都可以用channel来在各个Gouroutine之间传递。<br>
Channel的声明和初始化非常简单，同时需要指定Channel中所传递的数据的类型：
 ```
 var ch1 chan interface{} //声明一个传递类型为interface{}的channel
 ch2 := make(chan int)   //创建一个传递的数据类型为int的unbuffered channel
ch3 := make(chan string, 5) //创建一个传递string类型的buffered channel,buffer的容量为5
 ```
 这里要注意的是，已声明但是没有初始化的Channel，它的值是nil喔，nil的Channel无论发送和读取都会阻塞。后面的章节会列出几种使用Channel时的坑，并一一说明。<br>
 往Channel中写入及读取：
 ```
 ch1 <- x  //往Channel中写入值
y = <- ch2 //从channel中取出值
<- ch3 //取值并丢弃
close(ch3) //关闭channel
 ```
 以上是Channel的定义和基本使用方式，Golang官方以及中文社区都有大量的资料说明。在这里要展开深入讨论的呢，是Channel的正确打开方式。因为仅仅了解了channel怎样定义，怎样写入和读取，仍然是不足以让你写出正确的使用channel的Go语言的程序的。下面一部分，我们从channel的使用细节和实例，来看看它的正确打开方式是怎样的，在实际使用中有哪些坑需要注意。
<br>
## 深入理解Channel中的Buffer与阻塞
首先举个例子来描述Go语言中不同Goroutine的工作方式，比如一个进程中的两个Goroutine，简单来说，就像一个汽车工厂里的两个生产车间，同时在进行汽车部件的生产。每个车间在分到CPU时间片的时候呢，就可以将自己的生产过程继续下去。这两个车间中间是有墙分隔开的，而Channel，就是两个车间之间传递工具或者零件的一个小窗口。两个车间互相不可见，生产过程也互相不干扰，就通过小窗口来互相传递东西。在Goroutine中呢，可以用channel来传递消息，同步状态<br>
本章的概述里面提到过，channel定义的时候可以定义为有buffer的channel，或者无buffer的channel。这里面可是大有玄机，因为channel是会阻塞的，有buffer和无buffer的channel阻塞的条件和行为也大不一样。
### 无Buffer的Channel
无buffer的channel意味着没有缓冲，只有读写双方都准备好的时候，写操作和读操作才能完成，否则就一定会阻塞。实际上是什么意思呢？还是以上面那个例子来说，车间A和车间B之间有一个无buffer的channel叫ab，车间A生产过程中需要通过channel ab往车间B传递一个零件。这时候车间A的工人把一个零件放到这个小窗ab这，但是他不能放手啊，因为ab没有buffer，只有等对面B车间有一个人来接他手里的零件时，他才能松开手，回去继续车间A的生产过程。这就是无buffer的channel阻塞时的状态，写操作时，如果该channel没有被读取，那么写操作将一直被阻塞，直到读一方来读取它。<br>
而对于读操作，同样的，如果车间B的工人需要从Channel ab这个小窗取一个零件，取到了才能继续他的生产过程，那么他只能在窗口一直伸手等着，只有另一方来到小窗ab前，把他要取的零件放到他手上，这个取东西的操作才能结束。在此之前，B车间中的生产过程是必须等待的。<br>
### 有buffer的Channel
有buffer的Channel就有了缓冲区了，不像无buffer的channel，需要读写双方都要准备好才能完成读写操作，在有buffer的channel上的读写，只要buffer没满就不会阻塞。写的Goroutine可以放下就走，读的Goroutine可以取了就走。再以工厂车间为例，有buffer的Channel相当于两个车间通讯的小窗窗台上放了个盒子，buffer的大小就是盒子的容量。例如C车间和D车间，相互之间通过Channel cd通讯，假如定义cd时我们使用的buffer大小是10，那么意味着Channel cd这个小窗里有个大小为10的盒子，C车间的工人来小窗前给D车间传递零件时，他无须等D车间的人来取，只要往盒子里放就好了，当然前提是盒子里有空格子。如果盒子满了，那么C也必须等到D至少取走一个零件，盒子里空出一个格的时候才能往里放。<br>
简言之，对于带buffer的Channel的写入方，只有当buffer满了的时候会阻塞写入行为。对于读取方呢，在buffer为空时候会阻塞（和无buffer时行为相同），直到它能读到值才会往下走。
Buffer起什么作用，有无buffer的channel在行为上有什么区别。Channel的阻塞行为是什么样的。怎样正确使用Channel的阻塞。
## Channel的数据读取与生命周期管理
在前面讲Channel定义的时候我们介绍过从Channel中读取数据的最简单方式，其实在实际的程序中，读取channel往往是用于监听一些状态的变化和感知从其他Goroutine来的数据传递，既然使用了Goroutine并发地去运行程序，我们自然是希望各个Goroutine不受阻塞地做各自的处理，只有在监听的Channel里有当前Goroutine关心的数据到达时再做相应的处理。所以Channel的读取往往就不是简单的使用
了，需要做一些包装和改进来更好地利用其在Goroutine之间同步的能力
### 直接读取
前面总结过，直接读取即用Channel的读取操作符<-直接从Channel中读值，例如:<br>
```
 <- inbox
```
当Channel无buffer，或buffer中没有数据时，程序会在此处阻塞，直到有数据可读。在实际的Go程序中，还是有几种常用的模式来从Channel中读取数据的。对于一个同步执行的Goroutine来说，从Channel读取的场景包括程序等待同步状态，监听通知，获取数据等等。
### 用select读取
最常见的监听某个Channel的方式莫过于，另起一个Goroutine，在当中用for循环做select操作，循环地从Channel中读取数据，例如：<br>
```
func main() {
   ch1 := make(chan int)
	go func() {
		for {
			select {
			case val := <-ch1:
				fmt.Println("Received value: ", val)
			}
		}
	}()
	for i := 0; i <= 10; i++ {
		ch1 <- i * 2
	}
	fmt.Println("Process done.")
}
```
### 用for..range读取
用for...range读取的话对于无buffer的Channel行为与select方式一致，如下所示。
读取无buffer的Channel:
```
func main() {
  ch1 := make(chan int)
	go func() {
		fmt.Println("-- start read")
		for r := range ch1 {
			fmt.Println("Received value: ", r)
		}
	}()
	for i := 0; i <= 10; i++ {
		ch1 <- i * 2
	}
	fmt.Println("Process done.")
}
```
读取有buffer的Channel
```
func main() {
   ch1 := make(chan int, 20)
	for i := 0; i <= 10; i++ {
		ch1 <- i * 2
	}
	go func() {
		l := len(ch1)
		fmt.Println("-- start read, the length of channel is: ", l)
		for r := range ch1 {
			fmt.Printf("Received value: %d, the length is: %d\n", r, len(ch1))
		}
		fmt.Println("Drain channel out done.")
	}()

	fmt.Println("Process done.")
}
```
### Channel的关闭
关闭Channel即: <br>
```
close(ch)
```
但是Channel的关闭是不应当随意进行的，因为关闭Channel后的读和写的行为都需要我们深刻理解，才能选择在程序的适当的地方去做Channel的关闭操作。<br>
首先，已关闭的Channel永远不会阻塞。很好，不阻塞，至少在错误状态出现的时候程序不会block，错误比较容易被发现。说到这里，我们有必要总结一下Channel在几种特殊状态下的行为。
### 值得注意的几种特殊情况下的坑
这部分说一下Channel在几种特殊状态下的情况，使用时候要注意喔。其实这部分内容也是Dave Cheney在他的blog里说过的，我在这里总结一下。上面刚说过channel的关闭，那么对于已关闭的Channel的读写操作会是什么样的状况，怎样避免错误的使用以带来非预期的结果呢。
#### 写入已关闭的Channel
注意，向已关闭的Channel写入，会panic，会panic。例如这段代码，在关闭Channel后再往里写入值
```
ch1 := make(chan string, 1)
	go func() {
		v := <- ch1
		fmt.Println("Received from ch1: ", v)
	}()
	close(ch1)
	ch1 <- "Hi, nice to meet you"
	fmt.Println("Done")
```
会输出
```
panic: send on closed channel
```
[try](https://play.golang.org/p/ZT0moZyP-t)
#### 从已关闭的Channel读取
Channel关闭后从其中读取会是什么样的行为呢？一个好消息是，Channel关闭后，读取操作就再也不会block了。是不是感觉松一口气？对于无buffer的Channel，关闭后再读取，不会block，会读取到该Channel类型的零值。具体来讲什么意思呢，就是如果这个Channel是string类型的，就会读到"", 如果Channel是interface{}类型，那将会读到空指针nil，以此类推。<br>
对于buffered channel，关闭后读取操作依然可以把其中的数据读取出来，直到这个Channel被取空，当所有值都读完后，继续读该channel会得到该Channel的类型的零值数据。<br>
所以在这里我们很清楚地可以推断出，如果用select无限循环地读取，那么Channel被关闭后就会死循环了，永远能读到值，但每次取到的都是该Channel类型的零值。而如果用for...range方式来读取的话，循环就会在Channel中的数据被全部取出后结束。还有一种方式可以用来判断已关闭的Channel是否已经被取空，就是读取Channel时使用读取操作的第二个返回值，如下所示<br>
` x, ok := <-ch`<br>
当Channel被关闭并且为空的时候，ok为false。下面看看两种情况的例子
##### 用select读取
在常用的select方式读取已关闭的Channel会是怎样的行为呢？如上所述，Channel被关闭后不会block了，会永远返回零值，所以如果代码里不做判断的话将进入无限循环状态。还是用上面那段代码做例子
```
func main() {
   ch1 := make(chan int)
	go func() {
		for {
			select {
			case val := <-ch1:
				fmt.Println("Received value: ", val)
			}
		}
	}()
	for i := 0; i <= 10; i++ {
		ch1 <- i * 2
	}
	fmt.Println("Process done.")
}
```
没问题，正常输出了0-10的2倍
```
Received value:  0
Received value:  2
Received value:  4
Received value:  6
Received value:  8
Received value:  10
Received value:  12
Received value:  14
Received value:  16
Received value:  18
Received value:  20
Process done.
```
如果在发送完毕后关闭Channel了, 循环读取将进入无限循环状态
```
close(ch1)
fmt.Println("Process done.")
```
输出将变为：
```
... ...
Received value:  0
Received value:  0
Received value:  0
Received value:  0
Received value:  0
Received value:  0
Received value:  0
Received value:  0
......
```
死循环了，无论有buffer还是无buffer的Channel都一样。所以如果用select无限循环方式作为Channel接收的方式，可以利用读Channel的第二个返回值加一个判断来处理Channel关闭并被读取完的行为
```
func main() {
   ch1 := make(chan int)
	go func() {
		var closed bool
		for {
			if closed {
				break
			}
			select {
			case val, ok := <-ch1:
				fmt.Println("Received value: ", val)
				if !ok {
					closed = true
					break
				}
			}
		}
		fmt.Println("Channel is closed and drain out.")
	}()
	for i := 0; i <= 10; i++ {
		ch1 <- i * 2
	}
	close(ch1)
	fmt.Println("Process done.")
}
```
这时候的输出是
```
Received value:  0
Received value:  2
Received value:  4
Received value:  6
Received value:  8
Received value:  10
Received value:  12
Received value:  14
Received value:  16
Received value:  18
Received value:  20
Process done.
Received value:  0
Channel is closed and drain out.
```
使用有buffer的Channel时，行为也是类似的。
##### 用for...range读取
在Channel被关闭后，for...range方式就比较适合用来drain out Channel中的所有数据了，下面这段代码在发送完毕数据后关闭Channel，而读取方的for...range循环会在接收完所有数据之后退出循环。
```
func main() {
	ch1 := make(chan int, 20)
	for i := 0; i <= 10; i++ {
		ch1 <- i * 2
	}
	close(ch1)
	go func() {
		l := len(ch1)
		fmt.Println("-- start read, the length of channel is: ", l)
		for r := range ch1 {
			fmt.Printf("Received value: %d, the length is: %d\n", r, len(ch1))
		}
		fmt.Println("Drain channel out done.")
	}()

	fmt.Println("Process done.")
}
```
输出为:
```
Process done.
-- start read, the length of channel is:  11
Received value: 0, the length is: 10
Received value: 2, the length is: 9
Received value: 4, the length is: 8
Received value: 6, the length is: 7
Received value: 8, the length is: 6
Received value: 10, the length is: 5
Received value: 12, the length is: 4
Received value: 14, the length is: 3
Received value: 16, the length is: 2
Received value: 18, the length is: 1
Received value: 20, the length is: 0
Drain channel out done.
```

所以基于Channel关闭后的行为，怎样关闭它才是安全的呢？有一条比较通用的适用原则。即不要从接收端关闭channel，如果发送端多个并发发送者，那也不要从其中一个发送端去关闭channel。换句话说，如果发送者是唯一的sender，或者发送者是channel最后一个活跃的sender，那么应该在发送者的goroutine关闭channel，通过这个关闭行为来通知所有的接收者们已经没有值可以读了。只要在使用Channel时遵循这条原则，就可以保证永远不会发生向一个已经关闭的channel写入值或者关闭一个已经关闭的channel。

### nil的Channel
#### 写入
当一个Channel的值为nil时，写入这个Channel的操作会永远阻塞。例如下面这段代码：<br>
```
func main() {
        var c chan string
        c <- "hello, David" // block forever
}
```
#### 读取
同样的，从一个nil的Channel读取，也会永远阻塞:<br>
```
func main() {
        var c chan string
        str := <- c // block forever
}
```
一个声明后但是尚未通过make语句初始化的Channel的默认值就是其零值，也就是nil。所以在使用Channel时一定要注意这一点，在对其写入或者读取时一定要保证这个Channel已经被初始化了。阻塞是一种可怕的安静，没有任何错误，没有panic，进程还活着，但是当前Goroutine就陷入了无尽的等待中，无法继续往下走了，无法感知到。<br>
从Channel的原理上来看，怎样解释这种阻塞行为呢，Dave Cheney很好地给出了他的解释，我来简单描述一下: <br>
首先，Channel的类型定义并不包括buffer，所以buffer是Channel本身的值的一部分，Channel的声明只会声明其类型。Channel只是被声明而没有被初始化的时候，它的buffer size是0。这种情况下相当于是一个无buffer的Channel了，只有发送和接收双方都准备好的情况下，无buffer的Channel才不会阻塞。而对于未初始化的Channel，其值是nil，发送和接收双方都无法获取到对方的引用，无法和对方通讯，双方都被阻塞在这个未初始化的Channel里。
## 小结
本文结合日常工作中的开发实践，将Channel的正确使用方式和行为做了一个简单归纳，希望能帮助到各位进入Go语言的世界不久的开发者们少踩一些坑，更加高效而合理地使用Channel这一特性。
## 参考资料
https://dave.cheney.net/<br>
http://studygolang.com/articles/9518<br>
https://blog.golang.org/
