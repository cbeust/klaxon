<img src="doc/klaxon.png" alt="Klaxon logo" height="101" width="220" />

Klaxon is a library to parse JSON in Kotlin.

## Install

```kotlin
repositories {
    jcenter()
}

dependencies {
    compile 'com.beust:klaxon:2.0.1'
}
```

## Use

Klaxon has different API's depending on your needs:

- [An object binding API](#objectBindingApi) that lets you bind JSON documents directly to your objects, and vice versa.
- [A low level API](#lowLevelApi)that lets you manipulate JSON objects and use queries on them.
- [A streaming API](#streamingApi) so you can act on the JSON document as it's being read.

## <a name="objectBindingApi">Object binding API</a>

### General usage

To use Klaxon's high level API, you define your objects as nullable `var`'s:

```kotlin
data class Person(
    var name: String? = null,
    var age: Int? = null
)
```

You then specify the class of your object as a type parameter when invoking the `parse()` function:

```kotlin
    val result = Klaxon()
        .parse<Person>("""
        {
          "name": "John Smith",
          "age": 23
        }
    """)

    assertThat(result.name).isEqualTo("John Smith")
    assertThat(result.age).isEqualTo(23)
```

### Customizing field names

You can map names found in JSON with field names with the `@Json` annotation:

```kotlin
data class Person(
    @Json(name = "the_name")
    var name: String? = null,
)
```

```kotlin
    val result = Klaxon()
        .parse<Person>("""
        {
          "the_name": "John Smith", // note the field name
          "age": 23
        }
    """)

    assertThat(result.name).isEqualTo("John Smith")
    assertThat(result.age).isEqualTo(23)
```

### Custom types

Klaxon will do its best to initialize the objects with what it found in the JSON document but you can take control
of this mapping yourself by defining type converters.

The converter interface is as follows:

```kotlin
interface Converter<T> {
    fun toJson(value: T): String?
    fun fromJson(jv: JsonValue) : T
}
```

You define a class that implements this interface and implement the logic that converts your class to and from JSON.
For example, suppose you receive a JSON document with a field that can either be a `0` or a `1` and you want to
convert that field into your own type that's initialized with a boolean:

```kotlin
    class BooleanHolder(var flag: Boolean? = null)

    val myConverter = object: Converter<BooleanHolder> {
        override fun toJson(value: BooleanHolder): String?
            = """{"flag" : "${if (value.flag == true) 1 else 0}""""
    
        override fun fromJson(jv: JsonValue)
            = BooleanHolder(jv.objInt("flag") != 0)
    
    }
```

Next, you declare your converter to your `Klaxon` object before parsing:

```kotlin
    val result = Klaxon()
        .converter(myConverter)
        .parse<BooleanHolder>("""
            { "flag" : 1 }
        """)

    assertThat(result?.flag).isTrue()
```

### JsonValue

The `Converter` type passes you an instance of the [`JsonValue`](https://github.com/cbeust/klaxon/blob/master/src/main/kotlin/com/beust/klaxon/JsonValue.kt) class.
This class is a container of a Json value. It
is guaranteed to contain one and exactly one of either a number, a string, a character, a `JsonObject` or a `JsonArray`.
If one of these fields is set, the others are guaranteed to be `null`. Inspect that value in your converter to make
sure that the value you are expecting is present, otherwise, you can cast a `KlaxonException` to report the invalid
JSON that you just found.

### Field conversion overriding

It's sometimes useful to be able to specify a type conversion for a specific field without that conversion applying
to all types of your document (for example, your JSON document might contain various dates of different formats).
You can use field conversion types for this kind of situation.

Such fields are specified by your own annotation, which you need to specify as targetting a `FIELD`:

```kotlin
@Target(AnnotationTarget.FIELD)
annotation class KlaxonDate
```

Next, annotate the field that requires this specific handling in the constructor of your class. Do note that such
a constructor needs to be annotated with `@JvmOverloads`:

```kotlin
    class WithDate @JvmOverloads constructor(
        @KlaxonDate
        var date: LocalDateTime? = null,
    )
```

Define your type converter (which has the same type as the converters defined previously). In this case, we
are converting a `String` from JSON into a `LocalDateTime`:

```kotlin
val dateConverter = object: Converter<LocalDateTime> {
    override fun fromJson(jv: JsonValue) =
        if (jv.string != null) {
            LocalDateTime.parse(jv.string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } else {
            throw KlaxonException("Couldn't parse date: ${jv.string}")
        }

    override fun toJson(o: LocalDateTime)
            = """ { "date" : $o } """
```

Finally, declare the association between that converter and your annotation in your `Klaxon` object before parsing:

```kotlin
    val result = Klaxon()
        .fieldConverter(dateConverter)
        .parse<WithDate>("""
        {
          "theDate": "2017-05-10 16:30"
        }
    """)

    assertThat(result?.date).isEqualTo(LocalDateTime.of(2017, 5, 10, 16, 30))

``` 

## <a name="lowLevelApi">Low level API</a>

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

## <a name="advancedDsl">Advanced DSL</a>

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

Functions that you can use inside a `json {}` expression are defined in the [`KlaxonJson`](https://github.com/cbeust/klaxon/blob/master/src/main/kotlin/com/beust/klaxon/KlaxonJson.kt) class.

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

## <a name="streamingApi">Streaming API</a>

The streaming API is useful in a few scenarios:

- When your JSON document is very large and reading it all in memory might cause issues.
- When you want your code to react as soon as JSON values are being read, without waiting for the entire document
to be read.

This second point is especially important to make mobile apps as responsive as possible and make them less reliant
on network speed.

### Writing JSON

As opposed to conventional JSON libraries, Klaxon doesn't supply a `JsonWriter` class to create JSON documents since
this need is already covered by the `json()` function, documented in the [Advanced DSL](#advancedDsl) section.

### Reading JSON  

<TBD>

## Implementation

The Parser is implemented as a mutable state machine supported by a simplistic `State` monad, making the main loop very simple:

```kotlin
val stateMachine = StateMachine()
val lexer = Lexer(inputStream)
var world = World(Status.INIT)
do {
    val token = lexer.nextToken()
    world = stateMachine.next(world, token)
} while (token.tokenType != Type.EOF)
```

## Troubleshooting

Here are a few common errors and how to resolve them.

- `NoSuchMethodException: <init>`

You might see the following exception:

```kotlin
Caused by: java.lang.NoSuchMethodException: com.beust.klaxon.BindingAdapterTest$personMappingTest$Person.<init>()
	at java.lang.Class.getConstructor0(Class.java:3082)
	at java.lang.Class.newInstance(Class.java:412)
```

This is typically caused by your object class being defined inside a function (which makes its constructor require an
 additional parameter that Klaxon doesn't know how to fill).
 
Solution: move that class definition outside of the function.

## Limitations

* Currently reads the entire JSON content in memory, streaming is not available yet
* Error handling is very primitive



