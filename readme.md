# Jersey Todo API

## Usage

### Create a new todo item

```Shell
curl --data "title=Hello+World&body=Buy+some+bye+byes" http://localhost:5000/todo
```

It will return a response of the following type:

```javascript
{
	"id" : "5496de5fd4c6d2992e916299",
	"title" : "Hello World",
	"body" : "Buy some bye byes",
	"modification_token" : "6cnvgcejcvh60nlebvru6vc9ev",
	"done" : false
}
```

The modification token is only ever shown after the object creation. It is necessary in order to modify or delete items. 
All new todo items are not done by default. They can be set to done later on.

### Get an existing todo item

```
curl http://localhost:5000/todo/5496de5fd4c6d2992e916299
```

The response is a JSON representation of the object:

```javascript
{
	"id" : "5496de5fd4c6d2992e916299",
	"title" : "Hello World",
	"body" : "Buy some bye byes",
	"done" : false
}
```

Note that the modification token is not included.

### Get all todo items

```
curl http://localhost:5000/todo
```

Now, the response is no longer a JSON dictionary, but a JSON array containing all the todo items:

```javascript
[
	{
		"id" : "5496de5fd4c6d2992e916299",
		"title" : "Hello World",
		"body" : "Buy some bye byes",
		"done" : false
	}
]
```

### Modify an existing todo item

This method is used in order to change the title, the body, or the done status of an item. The values that

```
curl -X PUT --data "title=Hello+World+(modified)&done=true&modification_token=6cnvgcejcvh60nlebvru6vc9ev" http://localhost:5000/todo/5496de5fd4c6d2992e916299
```

The response is the new JSON representation of the todo item:
```javascript
{
	"id" : "5496de5fd4c6d2992e916299",
	"title" : "Hello World (modified)",
	"body" : "Buy some bye byes",
	"done" : true
}
```

