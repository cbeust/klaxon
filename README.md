Klaxon is a lightweight library to parse JSON in Kotlin.

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

```
    val ages = array.long("age")
    println("Ages: ${ages}")
```

Result:

```
Ages: JsonArray(value=[20, 25, 38])
```
