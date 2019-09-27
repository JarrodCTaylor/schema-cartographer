# Client

## Running Locally & Development

Run `clojure -Arepl script/repl.clj` and vist http://localhost:9500

## Sass Stylesheets (.scss)

Stylesheet source files are located in `/src/sass` and are written in [Sass](http://sass-lang.com/) `.scss` syntax.

Stylesheet Devlopment:

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

`$ npx webpack`

### Run Tests

While application is running tests are visible at:

`http://localhost:9500/figwheel-extra-main/auto-testing`

### Package For Deployment

- `rm -rf target/public`
- `cd src/sass; yarn css`
- `clojure -A:min`
