# Jersey Todo API

## Installation

- Make sure you have Maven installed and configured to use JDK 1.7 or bigger.
- Create an environment configuration file (`.env`) in the project root directory with the Twilio, MongoDB, and Searchly configuration variables. The following configuration properties have to be set:
	- `TWILIO_AUTH_TOKEN`
	- `MONGOSOUP_URL`
	- `SEARCHBOX_URL`
- Have the [Heroku command line tools](https://toolbelt.heroku.com/) installed

Compile the project:
```shell
mvn clean install
```

Run it locally:
```shell
foreman start web
```

The API should now be responding to HTTP requests to `http://localhost:5000`

## Usage

### Create a new todo item

```shell
curl --data "title=Hello+World&body=Buy+some+bye+byes" https://jersey-todo-api.herokuapp.com
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
All new todo items are not done by default. [They can be set to done later on](#modify-an-existing-todo-item).

### Get an existing todo item

```shell
curl https://jersey-todo-api.herokuapp.com/5496de5fd4c6d2992e916299
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

```shell
curl https://jersey-todo-api.herokuapp.com
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

This method is used in order to change the title, the body, or the done status of an item. The values that are to 
remain unchanged need not be specified at all.

```shell
curl -X PUT --data "title=Hello+World+(modified)&done=true&modification_token=6cnvgcejcvh60nlebvru6vc9ev" https://jersey-todo-api.herokuapp.com/5496de5fd4c6d2992e916299
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

### Subscribe to done status changes of a todo item

If you wish to be informed via SMS whenever a todo item is set to done (or not done), call the subscribe method:

```shell
curl https://jersey-todo-api.herokuapp.com/5496de5fd4c6d2992e916299/subscribe/+16509991234
```

Due to the fact that I am using a trial Twilio account, you will most likely get a response status code 500 and the
following error message:

> There was an issue with Twilio: The number +16509991234 is unverified. Trial accounts cannot send messages to 
unverified numbers; verify +16509991234 at twilio.com/user/account/phone-numbers/verified, or purchase a Twilio number 
to send messages to unverified numbers.

Verifying the number with Twilio would be the easiest remedy.

NOTE: **If a Twilio error is thrown, you are not added to the subscribers list.**

### Search existing todo items

```shell
curl https://jersey-todo-api.herokuapp.com/search/hell*
```

The titles and bodies of all existing todo items will be searched. The title matches are prioritized as being thrice as
relevant as body matches. The response looks just like the response for all objects, with the difference that not all
objects are shown and that they are ordered by decreasing relevance.

```javascript
[
	{
		"id" : "5496de5fd4c6d2992e916299",
		"title" : "Hello World (modified)",
		"body" : "Buy some bye byes",
		"done" : true
	}
]
```

### Remove a todo item

This request is formatted pretty much like the update request, with the sole difference that the modification token
is provided as a query parameter rather than within the request body.

```shell
curl -X DELETE https://jersey-todo-api.herokuapp.com/5496de5fd4c6d2992e916299?modification_token=6cnvgcejcvh60nlebvru6vc9ev
```

A successful item removal produces no content and an HTTP 204 No Content status code.
