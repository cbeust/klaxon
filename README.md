<img src="doc/klaxon.png" alt="Klaxon logo" height="101" width="220" />

Klaxon is a lightweight library to parse JSON in Kotlin.

## Install

```kotlin
repositories {
    jcenter()
}

dependencies {
    compile 'com.beust:klaxon:0.30'
}
```

## API

Values parsed from a valid JSON file can be of the following type:

* Int
* Long
* BigInteger
* String
* Double
* Boolean
* JsonObject
* JsonArray

`JsonObject` behaves like a `Map` while `JsonArray` behaves like a `List`. Once you have parsed a file, you should cast it to the type that you expect. For example, consider this simple file called `object.json`:

```json
{
    "firstName" : "Cedric",
    "lastName" : "Beust"
}
```

Since this is a JSON object, we parse it as follows:

```kotlin
fun parse(name: String) : Any? {
    val cls = Parser::class.java
    return cls.getResourceAsStream(name)?.let { inputStream ->
        return Parser().parse(inputStream)
    }
}

// ...

val obj = parse("/object.json") as JsonObject
```

Parse from String value :
```kotlin
val parser: Parser = Parser()
val stringBuilder: StringBuilder = StringBuilder("{\"name\":\"Cedric Beust\", \"age\":23}")
val json: JsonObject = parser.parse(stringBuilder) as JsonObject
println("Name : ${json.string("name")}, Age : ${json.int("age")}")
```
Result :
```
Name : Cedric Beust, Age : 23
```

You can also access the JSON content as a file, or any other resource you can get an `InputStream` from.

Let's query these values:

```kotlin
    val firstName = obj.string("firstName")
    val lastName = obj.string("lastName")
    println("Name: $firstName $lastName")

    // Prints: Name: Cedric Beust
```

`JsonObject` implements the following methods:

```kotlin
    fun int(fieldName: String) : Int?
    fun long(fieldName: String) : Long?
    fun bigInt(fieldName: String) : BigInteger?
    fun string(fieldName: String) : String?
    fun double(fieldName: String) : Double?
    fun boolean(fieldName: String) : Boolean?
    fun obj(fieldName: String) : JsonObject?
    fun <T> array(thisType: T, fieldName: String) : JsonArray<T>?
```

`JsonArray` implements the same methods, except that they return `JsonArray`s of the same type. This allows you to easily fetch collections of fields or even sub-objects. For example, consider the following:

```json
[
    {
        "name" : "John",
        "age" : 20
    },
    {
        "name" : "Amy",
        "age" : 25
    },
    {
        "name" : "Jessica",
        "age" : 38
    }
]
```

We can easily collect all the ages as follows:

```kotlin
val array = parse("/e.json") as JsonArray<JsonObject>

val ages = array.long("age")
println("Ages: $ages")

// Prints: Ages: JsonArray(value=[20, 25, 38])
```

Since a `JsonArray` behaves like a `List`, we can apply closures on them, such as `filter`:

```kotlin
val oldPeople = array.filter {
    it.long("age")!! > 30
}
println("Old people: $oldPeople")

// Prints: Old people: [JsonObject(map={age=38, name=Jessica})]
```

Let's look at a more complex example:

```json
[
    {
        "first": "Dale",
        "last": "Cooper",
        "schoolResults" : {
            "scores": [
                { "name": "math", "grade" : 90 },
                { "name": "physics", "grade" : 50 },
                { "name": "history", "grade" : 85 }
            ],
            "location" : "Berkeley"
        }
    },
    {
        "first": "Kara",
        "last": "Thrace",
        "schoolResults" : {
            "scores": [
                { "name": "math", "grade" : 75 },
                { "name": "physics", "grade" : 90 },
                { "name": "history", "grade" : 55 }
            ],
            "location" : "Stanford"
        }
    },
    {
        "first": "Jack",
        "last": "Harkness",
        "schoolResults" : {
            "scores": [
                { "name": "math", "grade" : 40 },
                { "name": "physics", "grade" : 82 },
                { "name": "history", "grade" : 60 }
            ],
            "location" : "Berkeley"
        }
    }
]
```

Let's chain a few operations, for example, finding the last names of all the people who studied in Berkeley:

```kotlin
println("=== Everyone who studied in Berkeley:")
val berkeley = array.filter {
    it.obj("schoolResults")?.string("location") == "Berkeley"
}.map {
    it.string("last")
}
println("$berkeley")

// Prints:
// === Everyone who studied in Berkeley:
// [Cooper, Harkness]
```

All the grades over 75:

```kotlin
println("=== All grades bigger than 75")
val result = array.flatMap {
    it.obj("schoolResults")
            ?.array<JsonObject>("scores")?.filter {
                it.long("grade")!! > 75
            }!!
}
println("Result: $result")

// Prints:
// === All grades bigger than 75
// Result: [JsonObject(map={name=math, grade=90}), JsonObject(map={name=history, grade=85}), JsonObject(map={name=physics, grade=90}), JsonObject(map={name=physics, grade=82})]

```

Note the use of `flatMap` which transforms an initial result of a list of lists into a single list. If you use `map`, you will get a list of three lists:

```kotlin
// Using map instead of flatMap
// Prints:
// Result: [[JsonObject(map={name=math, grade=90}), JsonObject(map={name=history, grade=85})], [JsonObject(map={name=physics, grade=90})], [JsonObject(    map={name=physics, grade=82})]]
```

## Pretty printing

You can convert any `JsonObject` to a valid JSON string by calling `toJsonString()` on it. If you want to get pretty-printed
version then you can call `toJsonString(true)`

## Advanced DSL

Creating a JSON object with the Klaxon DSL makes it possible to insert arbitrary pieces of Kotlin code anywhere you want. For example, the following creates an object that maps each number from 1 to 3 with its string key:

```kotlin
val logic = json {
    array(listOf(1,2,3).map {
        obj(it.toString() to it)
    })
}
println("Result: ${logic.toJsonString()}")
```

will output:

```text
Result: [ { "1" : 1 }, { "2" : 2 }, { "3" : 3 }  ]
```

## Flattening and path lookup

If we have the following JSON
```json
{
	"users" : [
	    {
	        "email" : "user@is.here"
	    },
	    {
	    	"email" : "spammer@there.us"
	    }
	]
}
```

We can find all emails by

```kotlin
(parse("my.json") as JsonObject).lookup<String?>("users.email")
```

## Implementation

The Parser is implemented as a mutable state machine supported by a simplistic `State` monad, making the main loop very simple:

```kotlin
val sm = StateMachine()
val lexer = Lexer(inputStream)
var world = World(Status.INIT)
do {
    val token = lexer.nextToken()
    world = sm.next(world, token)
} while (token.tokenType != Type.EOF)
```

## Limitations

* Currently reads the entire JSON content in memory. Streaming will be available soon for large files.
* Error handling is very primitive


