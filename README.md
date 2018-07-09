
<img src="doc/klaxon.png" alt="Klaxon logo" height="101" width="220" />

Klaxon is a library to parse JSON in Kotlin.

## Install

```kotlin
repositories {
    jcenter()
}

dependencies {
    implementation 'com.beust:klaxon:3.0.1'
}
```

## Community

Join the [`#klaxon` Slack channel](https://kotlinlang.slack.com/messages/C90AVCDQU/).

## Use

Klaxon has different API's depending on your needs:

- [An object binding API](#objectBindingApi) to bind JSON documents directly to your objects, and vice versa.
- [A streaming API](#streamingApi) to process your JSON documents as they're being read.
- [A low level API](#lowLevelApi) to manipulate JSON objects and use queries on them.
- [A JSON path query API](#jsonPath) to extract specific parts of your JSON document while streaming.

These four API's cover various scenarios and you can decide which one to use based on whether you want
to stream your document and whether you need to query it.

|                     | Streaming | Query        | Manipulation |
|---------------------|-----------|--------------|--------------|
| Object binding API  | No        | No           | Kotlin objects |
| Streaming API       | Yes       | No           | Kotlin objects and JsonObject/JsonArray |
| Low level API       | No        | Yes          | Kotlin objects |
| JSON Path query API | Yes       | Yes          | JsonObject/JsonArray |


## <a name="objectBindingApi">Object binding API</a>

### General usage


To use Klaxon's high level API, you define your objects inside a class. Klaxon supports all the classes you can
define in Kotlin:

- Regular and `data` classes.
- Mutable and immutable classes.
- Classes with default parameters.

For example:

```kotlin
class Person(val name: String, val age: Int)
```

Classes with default parameters are supported as well:

```kotlin
class Person (val name: String, var age: Int = 23)
```

Once you've specified your value class, you invoke the `parse()` function, parameterized with that class:

```kotlin
val result = Klaxon()
    .parse<Person>("""
    {
      "name": "John Smith",
    }
    """)

assert(result.name == "John Smith")
assert(result.age == 23)
```

### The @Json annotation

The `@Json` annotation allows you to customize how the mapping between your JSON documents and
your Kotlin objects is performed. It supports the following attributes:

#### `name`

Use the `name` attribute when your Kotlin property has a different name than the field found in your
JSON document:

```kotlin
data class Person(
    @Json(name = "the_name")
    val name: String
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

assert(result.name == "John Smith")
assert(result.age == 23)
```

#### `ignored`

You can set this boolean attribute to `true` if you want certain properties of your value class not to be
mapped during the JSON parsing process. This is useful if you defined additional properties in your value classes.

```kotlin
class Ignored(val name: String) {
   @Json(ignored = true)
   val actualName: String get() = ...
}
```

In this example, Klaxon will not try to find a field called `actualName` in your JSON document.

Note that you can achieve the same result by declaring these properties `private`:

```kotlin
class Ignored(val name: String) {
   private val actualName: String get() = ...
}
```

Additionally, if you want to declare a property `private` but still want that property to be visible to
Klaxon, you can annotate it with `@Json(ignored = false)`.

### Custom types

Klaxon will do its best to initialize the objects with what it found in the JSON document but you can take control
of this mapping yourself by defining type converters.

The converter interface is as follows:

```kotlin
interface Converter {
    fun canConvert(cls: Class<*>) : Boolean
    fun toJson(value: Any): String
    fun fromJson(jv: JsonValue) : Any
}
```

You define a class that implements this interface and implement the logic that converts your class to and from JSON.
For example, suppose you receive a JSON document with a field that can either be a `0` or a `1` and you want to
convert that field into your own type that's initialized with a boolean:

```kotlin
class BooleanHolder(val flag: Boolean)

val myConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
        = cls == BooleanHolder::class.java

    override fun toJson(value: Any): String
        = """{"flag" : "${if ((value as BooleanHolder).flag == true) 1 else 0}"""

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

assert(result.flag)
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
    val date: LocalDateTime
)
```

Define your type converter (which has the same type as the converters defined previously). In this case, we
are converting a `String` from JSON into a `LocalDateTime`:

```kotlin
val dateConverter = object: Converter {
    override fun canConvert(cls: Class<*>)
        = cls == LocalDateTime::class.java

    override fun fromJson(jv: JsonValue) =
        if (jv.string != null) {
            LocalDateTime.parse(jv.string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } else {
            throw KlaxonException("Couldn't parse date: ${jv.string}")
        }

    override fun toJson(o: Any)
            = """ { "date" : $o } """
}
```

Finally, declare the association between that converter and your annotation in your `Klaxon` object before parsing:

```kotlin
val result = Klaxon()
    .fieldConverter(KlaxonDate::class, dateConverter)
    .parse<WithDate>("""
    {
      "theDate": "2017-05-10 16:30"
    }
""")

assert(result?.date == LocalDateTime.of(2017, 5, 10, 16, 30))
```

### Property strategy

You can instruct Klaxon to dynamically ignore properties with the `PropertyStrategy` interface:

```kotlin
interface PropertyStrategy {
    /**
     * @return true if this property should be mapped.
     */
    fun accept(property: KProperty<*>): Boolean
}
```

This is a dynamic version of `@Json(ignored = true)`, which you can register with your `Klaxon` instance with the function `propertyStrategy()`:

```kotlin
val ps = object: PropertyStrategy {
    override fun accept(property: KProperty<*>) = property.name != "something"
}
val klaxon = Klaxon().propertyStrategy(ps)
```

You can define multiple `PropertyStrategy` instances, and in such a case, they all need to return `true` for a property to be included.

## <a name="streamingApi">Streaming API</a>

The streaming API is useful in a few scenarios:

- When your JSON document is very large and reading it all in memory might cause issues.
- When you want your code to react as soon as JSON values are being read, without waiting for the entire document
to be parsed.

This second point is especially important to make mobile apps as responsive as possible and make them less reliant
on network speed.  

Note: the streaming API requires that each value in the document be handled by the reader.  If you are simply
looking to extract a single value the [`PathMatcher API`](#jsonPath) may be a better fit.

### Writing JSON with the streaming API

As opposed to conventional JSON libraries, Klaxon doesn't supply a `JsonWriter` class to create JSON documents since
this need is already covered by the `json()` function, documented in the [Advanced DSL](#advancedDsl) section.

### Reading JSON with the streaming API

Streaming JSON is performed with the `JsonReader` class. Here is an example:

```kotlin
val objectString = """{
     "name" : "Joe",
     "age" : 23,
     "flag" : true,
     "array" : [1, 3],
     "obj1" : { "a" : 1, "b" : 2 }
}"""

JsonReader(StringReader(objectString)).use { reader ->
    reader.beginObject() {
        var name: String? = null
        var age: Int? = null
        var flag: Boolean? = null
        var array: List<Any> = arrayListOf<Any>()
        var obj1: JsonObject? = null
        while (reader.hasNext()) {
            val readName = reader.nextName()
            when (readName) {
                "name" -> name = reader.nextString()
                "age" -> age = reader.nextInt()
                "flag" -> flag = reader.nextBoolean()
                "array" -> array = reader.nextArray()
                "obj1" -> obj1 = reader.nextObject()
                else -> Assert.fail("Unexpected name: $readName")
            }
        }
    }
}
```

There are two special functions to be aware of: `beginObject()` and `beginArray()`. Use these functions
when you are about to read an object or an array from your JSON stream. These functions will make sure
that the stream is correctly positioned (open brace or open bracket) and once you are done consuming
the content of that entity, the functions will make sure that your object is correctly closed (closing brace
or closing bracket). Note that these functions accept a closure as an argument, so there are no `closeObject()/closeArray()` functions.

It is possible to mix both the object binding and streaming API's, so you can benefit from the best of both worlds.

For example, suppose your JSON document contains an array with thousands of elements in them, each of these elements
being an object in your code base. You can use the streaming API to consume the array one element at a time and then
use the object binding API to easily map these elements directly to one of your objects:

```kotlin
data class Person(val name: String, val age: Int)
val array = """[
        { "name": "Joe", "age": 23 },
        { "name": "Jill", "age": 35 }
    ]"""

fun streamingArray() {
    val klaxon = Klaxon()
    JsonReader(StringReader(array)).use { reader ->
        val result = arrayListOf<Person>()
        reader.beginArray {
            while (reader.hasNext()) {
                val person = klaxon.parse<Person1>(reader)
                result.add(person)
            }
        }
    }
}
```
## <a name="jsonPath">JSON Path Query API</a>

The [JSON Path specification](https://github.com/json-path/JsonPath) defines how to locate elements inside
a JSON document. Klaxon allows you to define path matchers that can match specific elements in your
document and receive a callback each time a matching element is found.

Consider the following docoument:

```json
{
   "library": {
       "books": [
           {
               "author": "Herman Melville",
               "title": "Moby Dick"
           },
           {
               "author": "Jules Vernes",
               "title": "L'île mystérieuse"
           }
       ]
   }
}
```

According to the JSON Path spec, the two authors have the following JSON paths:

```
$.library.books[0].author
$.library.books[1].author
```

We'll define a [`PathMatcher`](https://github.com/cbeust/klaxon/blob/master/src/main/java/com/beust/klaxon/PathMatcher.kt) that uses a regular expression to filter only the elements we want:

```kotlin
val pathMatcher = object : PathMatcher {
    override fun pathMatches(path: String) = Pattern.matches(".*library.*books.*author.*", path)

    override fun onMatch(path: String, value: Any) {
        println("Adding $path = $value")
    }
}

Klaxon()
    .pathMatcher(pathMatcher)
    .parseJsonObject(document)
```

Output:

```
Adding $.library.books[0].author = Herman Melville
Adding $.library.books[1].author = Jules Vernes
```

Two notes:
- Klaxon doesn't support the JSON Path expression language, only the element location specification.
- This API is streaming: your path observers will be notified as soon as a matching element has been
detected and its value completely parsed.


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

You can convert any `JsonObject` to a valid JSON string by calling `toJsonString()` on it. If you want to get a pretty-printed
version of that string, call `toJsonString(true)`

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
