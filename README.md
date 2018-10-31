# Optical character recognition server

Optical character recognition server project implements backend part of ocr project, and is hosted on server-lib.

### Installing

Clone project from git by executing:

```
git clone git@github.com:VladimirMarkovic86/ocr-server.git

or

git clone https://github.com/VladimirMarkovic86/ocr-server.git
```

After that execute command:

```
cd ocr-server
```

Add following line in hosts file:

```
127.0.0.1 ocr
```

and run project with this command:

```
lein run
```

By default project listens on port 1602, so you can make requests on https://ocr:1602 address.

**For purpose of making requests ocr-client was made and you should start up ocr-client also.**

**MongoDB server should also be up and running and its URI link should be set in PROD_MONGODB environment variable**

## Authors

* **Vladimir Markovic** - [VladimirMarkovic86](https://github.com/VladimirMarkovic86)

## License

This project is licensed under the Eclipse Public License 1.0 - see the [LICENSE](LICENSE) file for details

