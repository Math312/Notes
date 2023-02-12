# Go Channel Introduction

The channel in "go" looks like a "Message Queue". Recently we want to use it deeply, so I write that blog to detail the `Channel's` implementation.

At first, I will give you a simple example, as follows.

```go
package main

func main() {
	ch := make(chan int, 10)
	ch <- 1
	<-ch
	return
}
```

In this example, we do three actions in this code:

1. Create a new Channel `ch` at line 4
2. Send the message `1` to the `ch` at line 5
3. Receive the message `1` at line 6，but we drop it.

Now, let's call our old friend `delve` to help us analyze these code. If you have no knowledge in `delve`, you can refer to [the github of delve](https://github.com/go-delve/delve).

## The Exploration Using Delve

The goal of using delve is to disassemble the example code. You can open the directory of the example code and enter the following command. 

```shell
dlv debug

# to breakpoint the line 4
b main.go:4

# run to the breakpoint
c

# disassemble the code
disassemble
```

We can get the disassembled code as below.

```
TEXT main.main(SB) D:/Code/go_learn/main.go                                          
        main.go:3       0x942b40        493b6610        cmp rsp, qword ptr [r14+0x10]
        main.go:3       0x942b44        7649            jbe 0x942b8f                 
        main.go:3       0x942b46        4883ec20        sub rsp, 0x20                
        main.go:3       0x942b4a        48896c2418      mov qword ptr [rsp+0x18], rbp
        main.go:3       0x942b4f        488d6c2418      lea rbp, ptr [rsp+0x18]      
=>      main.go:4       0x942b54*       488d0545470000  lea rax, ptr [rip+0x4745]    
        main.go:4       0x942b5b        bb0a000000      mov ebx, 0xa                 
        main.go:4       0x942b60        e8bb1bfaff      call $runtime.makechan       
        main.go:4       0x942b65        4889442410      mov qword ptr [rsp+0x10], rax
        main.go:5       0x942b6a        488d1d575f0100  lea rbx, ptr [rip+0x15f57]
        main.go:5       0x942b71        e86a1efaff      call $runtime.chansend1
        main.go:6       0x942b76        488b442410      mov rax, qword ptr [rsp+0x10]
        main.go:6       0x942b7b        31db            xor ebx, ebx
        main.go:6       0x942b7d        0f1f00          nop dword ptr [rax], eax
        main.go:6       0x942b80        e8bb28faff      call $runtime.chanrecv1
        main.go:7       0x942b85        488b6c2418      mov rbp, qword ptr [rsp+0x18]
        main.go:7       0x942b8a        4883c420        add rsp, 0x20
        main.go:7       0x942b8e        c3              ret
        main.go:3       0x942b8f        e86c86ffff      call $runtime.morestack_noctxt
```

**Summary**:From the disassembled code, we can draw lots of conclusions:

1. The logic of creating channel in `$runtime.makechan `
2. The logic of sending message to channel in `$runtime.chansend1`
3. The logic of receiving message from channel in `$runtime.chanrecv1`

## Creating Channel(runtime.makechan)

The function of `runtime.makechan` locate in `runtime\chan.go` file. The function signature is as follow.

```go
func makechan(t *chantype, size int) *hchan
```

As we all know, the channel has it's own type. In our example, the channel type is `chan int`. The parameter `t *chantype` is utilized to record the type data. For another, the parameter `size int` record the size of the channel. In our example is `10`.If you are interested in `chantype`, there are the source of it. 

```go
type chantype struct {
	typ  _type
	elem *_type
	dir  uintptr
}

type _type struct {
	size       uintptr
	ptrdata    uintptr // size of memory prefix holding all pointers
	hash       uint32
	tflag      tflag
	align      uint8
	fieldAlign uint8
	kind       uint8
	// function for comparing objects of this type
	// (ptr to object A, ptr to object B) -> ==?
	equal func(unsafe.Pointer, unsafe.Pointer) bool
	// gcdata stores the GC type data for the garbage collector.
	// If the KindGCProg bit is set in kind, gcdata is a GC program.
	// Otherwise it is a ptrmask bitmap. See mbitmap.go for details.
	gcdata    *byte
	str       nameOff
	ptrToThis typeOff
}
```

But now, what we want to highlight is the process of creating a new channel. So let's talk about the detail of  function `runtime.makechan`. 

First, what will we get if we create a new channel?  Whether there is a data structure to represent a new channel? 

The answer to that question is hidden in the function signature. The return value of `runtime.makechan` is `*hchan`. Actually we can suspict `the hchan` is the data structure to represent the channel. To prove it, let's analyze the detail of function.

```go
func makechan(t *chantype, size int) *hchan {
       // # part 1
	elem := t.elem

	// compiler checks this but be safe.
	if elem.size >= 1<<16 {
		throw("makechan: invalid channel element type")
	}
	if hchanSize%maxAlign != 0 || elem.align > maxAlign {
		throw("makechan: bad alignment")
	}

	mem, overflow := math.MulUintptr(elem.size, uintptr(size))
	if overflow || mem > maxAlloc-hchanSize || size < 0 {
		panic(plainError("makechan: size out of range"))
	}

	// Hchan does not contain pointers interesting for GC when elements stored in buf do not contain pointers.
	// buf points into the same allocation, elemtype is persistent.
	// SudoG's are referenced from their owning thread so they can't be collected.
	// TODO(dvyukov,rlh): Rethink when collector can move allocated objects.
        // # part 2
	var c *hchan
	switch {
	case mem == 0:
		// Queue or element size is zero.
		c = (*hchan)(mallocgc(hchanSize, nil, true))
		// Race detector uses this location for synchronization.
		c.buf = c.raceaddr()
	case elem.ptrdata == 0:
		// Elements do not contain pointers.
		// Allocate hchan and buf in one call.
		c = (*hchan)(mallocgc(hchanSize+mem, nil, true))
		c.buf = add(unsafe.Pointer(c), hchanSize)
	default:
		// Elements contain pointers.
		c = new(hchan)
		c.buf = mallocgc(mem, elem, true)
	}
        // # part 3
	c.elemsize = uint16(elem.size)
	c.elemtype = elem
	c.dataqsiz = uint(size)
	lockInit(&c.lock, lockRankHchan)

	if debugChan {
		print("makechan: chan=", c, "; elemsize=", elem.size, "; dataqsiz=", size, "\n")
	}
	return c
}
```

We divide these code into 3 parts, and the label used to devide is signed in the code.

1. Part 1. Checking the legitimacy of input parameters and calculating the memory size of new channel need.
2. Part 2. According to the result of Part 1, Allocate the memory for channel.
3. Part 3. Allocate the lock for new channel. 

The result of these process is record in `hchan` structure.

```go
type hchan struct {
	qcount   uint           // total data in the queue
	dataqsiz uint           // size of the circular queue
	buf      unsafe.Pointer // points to an array of dataqsiz elements
	elemsize uint16
	closed   uint32
	elemtype *_type // element type
	sendx    uint   // send index
	recvx    uint   // receive index
	recvq    waitq  // list of recv waiters
	sendq    waitq  // list of send waiters

	// lock protects all fields in hchan, as well as several
	// fields in sudogs blocked on this channel.
	//
	// Do not change another G's status while holding this lock
	// (in particular, do not ready a G), as this can deadlock
	// with stack shrinking.
	lock mutex
}

type waitq struct {
	first *sudog
	last  *sudog
}

// Mutual exclusion locks.  In the uncontended case,
// as fast as spin locks (just a few user-level instructions),
// but on the contention path they sleep in the kernel.
// A zeroed Mutex is unlocked (no need to initialize each lock).
// Initialization is helpful for static lock ranking, but not required.
type mutex struct {
	// Empty struct if lock ranking is disabled, otherwise includes the lock rank
	lockRankStruct
	// Futex-based impl treats it as uint32 key,
	// while sema-based impl as M* waitm.
	// Used to be a union, but unions break precise GC.
	key uintptr
}
```

We can find that the rank of lock is `lockRankHchan` and the document tell us `lock protects all fields in hchan, as well as several fields in sudogs blocked on this channel.`

**Summary**: According to the analysis of `runtime.makechan`, we can get those data:

1. The message is stored in a fixed size circular queue.
2. Recv waiters and send waiters are stored in 2 queue
3. Sendx and recvx control the location of sending data and receiving data
4. The lock in channel protects all fields in hchan, as well as several fields in sudogs blocked on this channel. That lock is a channel-level lock.


## Sending Message To Channel (runtime.chansend1)

The function `runtime.chansend1`  contains the logic of sending message to channel. The function is as follows.

```go
// entry point for c <- x from compiled code
//
//go:nosplit
func chansend1(c *hchan, elem unsafe.Pointer) {
	chansend(c, elem, true, getcallerpc())
}
```

From the source code, it's not diffcult to recongize that the function `chansend(...)` proxy the main logic of `chansend1`.

```go
func chansend(c *hchan, ep unsafe.Pointer, block bool, callerpc uintptr) bool
```

To simplify the work of reading source code, we still divide the code into several parts.

1. Sending message to `Nil Channel`, program will park
   ```go
    if c == nil {
		if !block {
			return false
		}
		gopark(nil, nil, waitReasonChanSendNilChan, traceEvGoStop, 2)
		throw("unreachable")
	}
   ```

   At the beginning of this function, we can find the check for `channel is nil`. If the channel is nil, the goroutine will park.

   If you are interested in that case, this example will help you.

   ```go
    package main

    func main() {
        var ch chan int
        //ch := make(chan int, 2)
        ch <- 1
        ch <- 1
        ch <- 1
        <-ch
        return
    }
   ```
   You can debug that code, and you will find the program will run to the `gopark(..)` and then there is no runnable goroutine. In this case(`there is no runnable goroutine`), go will tell us that error.

   ```
    fatal error: all goroutines are asleep - deadlock!
   ```

2. The fast end for sending in unblocking channel.

    ```go
    if !block && c.closed == 0 && full(c) {
		return false
	}

    // full reports whether a send on c would block (that is, the channel is full).
    // It uses a single word-sized read of mutable state, so although
    // the answer is instantaneously true, the correct answer may have changed
    // by the time the calling function receives the return value.
    func full(c *hchan) bool {
        // c.dataqsiz is immutable (never written after the channel is created)
        // so it is safe to read at any time during channel operation.
        if c.dataqsiz == 0 {
            // Assumes that a pointer read is relaxed-atomic.
            return c.recvq.first == nil
        }
        // Assumes that a uint read is relaxed-atomic.
        return c.qcount == c.dataqsiz
    }
    ```

    If the channel is unblocking, unclosed, and full, we can't send a new message into it. Attention, there is no lock around these check code. So let's begin to check the correctness for no lock.

    At first, the channel is unblocking. And then let's talk about the variables involved in this check.

    1. Channel's closed status. It will change with time.
    2. Channel's dataqsiz. c.dataqsiz is immutable.
    3. Channel's recvq.first. It will change with time.
    4. Channel's message count. It will change with time.

    So, the unlocked situation influence the variable 1,3,4.

    1. If the channel is closed, we can't send message into it.

## Receiving Message From Channel (runtime.chanrecv1)