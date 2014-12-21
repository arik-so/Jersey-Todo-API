# Jersey Todo API

Usage
------------

> Get all todo items

```
curl http://localhost:5000/todo
```

Create a new todo item
-----------

```
curl --data "title=Hello+World&body=Buy+some+bye+byes" http://localhost:5000/todo
```

It will return a response of the following type:

```
{
	"id":"5496de5fd4c6d2992e916299",
	"title":"Hello World",
	"body":"Buy some bye byes",
	"modification_token":"6cnvgcejcvh60nlebvru6vc9ev",
	"done":false
}
```

The modification token is only ever shown after the object creation. It is necessary in order to modify or delete items.

