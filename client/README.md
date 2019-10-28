# Client

## Development

### Make sure you have shadow-cljs installed

```
npm install -g shadow-cljs
```

### Running locally

* cd `/src/sass`
* `yarn install`
* `yarn css`
* In client dir.
* `yarn install`
* Start application `clojure -Adev`
* App will be running at `http://localhost:9875/#/`
* Connect to repl `9001`
* Run `(shadow/repl :app)` from `script/repl.clj`

### Run Tests

``` sh
clj -A:test
```

Then visit:

`http://localhost:8021/`


## Sass Stylesheets (.scss)

Stylesheet source files are located in `/src/sass` and are written in [Sass](http://sass-lang.com/) `.scss` syntax.

Stylesheet Development:

``` sh
# Install dependencies
cd src/sass
yarn install

# Compile CSS once, while in /sass dir
yarn css

# Compile and watch, while in /sass dir
yarn watch:css
```

### Add NPM Modules

Add necessary modules to `package.json`

`$ yarn install`

### Package For Deployment

* `cd src/sass; yarn css`
* `clojure -Amin`
* The app will be in `resources/public/`

