Klaxon is a lightweight library to parse JSON in Kotlin.

Values parsed from a valid JSON file can be of the following type:

* Long
* String
* Double
* Boolean
* JsonObject
* JsonArray

`JsonObject` behaves like a `Map` while `JsonArray` behaves like a `List`. Once you have parsed a file, you should cast it to the type that you expect. For example, consider this simple file called `object.json`:

```
{
    "firstName" : "Cedric",
    "lastName" : "Beust"
}
```

Since this is a JSON object, we parse it as follows:

```
fun parse(name: String) : Any {
    val cls = javaClass<Parser>()
    val inputStream = cls.getResourceAsStream(name)!!
    return Parser().parse(inputStream)!!
}

// ...

    val obj = parse("/object.json") as JsonObject
```

Let's query these values:

```
    val firstName = obj.string("firstName")
    val lastName = obj.string("lastName")
    println("Name: ${firstName} ${lastName}")

    // Prints: Name: Cedric Beust
```

`JsonObject` implements the following methods:

```
    fun long(fieldName: String) : Long?
    fun string(fieldName: String) : String?
    fun double(fieldName: String) : Double?
    fun boolean(fieldName: String) : Boolean?
    fun obj(fieldName: String) : JsonObject?
    fun <T> array(thisType: T, fieldName: String) : JsonArray<T>?
```

`JsonArray` implements the same methods, except that they return `JsonArray`s of the same type. This allows you to easily fetch collections of fields or even sub-objects. For example, consider the following:

```
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

```
    val array = parse("/e.json") as JsonArray<JsonObject>

    val ages = array.long("age")
    println("Ages: ${ages}")

    // Prints: Ages: JsonArray(value=[20, 25, 38])
```

Since a `JsonArray` behaves like a `List`, we can apply closures on them, such as `filter`:

```
    val oldPeople = array.filter {
        it.long("age")!! > 30
    }
    println("Old people: ${oldPeople}")

    // Prints: Old people: [JsonObject(map={age=38, name=Jessica})]
```

