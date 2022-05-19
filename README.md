# Sample BigCommerce App with Scala and React
This is a sample project setup that demonstrates creating a BigCommerce app using Scala with Akka HTTP for the back-end and React for the front-end. This app is set up to use Heroku as the hosting service, but can be modified to use any host that fits your needs.

## Prerequisites
- Have **Scala**, **Java 11**, and **NodeJS** installed (see the [Scala Getting Started Guide](https://docs.scala-lang.org/getting-started/index.html))
- Create your own fork of this repository
- Clone your fork to your local machine

## Initial Setup
1. Create a **Draft App** in the [BigCommerce Devtools Portal](https://devtools.bigcommerce.com)
2. Setup your app in the devtools portal with the proper OAuth scopes. (Products - Modify)
3. Get your app's **Client ID** and **Client Secret** from the devtool portal and copy them into the `src/main/resources/application.conf` file.
4. Set up the `application.conf` file:
   - Set the `callback` value to the install route that you plan to use (i.e. https://yoursubdomain.ngrok.io/auth/install)
   - Modify the `database.properties` parameters accordingly with your database info by setting the `url` property. (ex. `url = "postgres://user:pass@localhost:5432/database"`)
5. Run `npm ci` in the `react` directory to install the needed node packages

## Building the Client
To create the React build, run `npm run build` in the `react` directory. This will build the React files into the `src/main/resources/client` directory so that the Scala app can serve the static React files.

## Client Development
By default, the React client will run on port `3000`, while the Scala server will run on port `8080`. To work on the client and the server at the same time, do the following:
- Run `sbt run` (or `sbt ~reStart` for auto reloading) in the project root directory to run the Scala server
- In a separate terminal, run `npm start` in the `/react` directory.
- The Scala back-end requires user authentication to communicate with the BigCommerce API. In order to bypass this, you can modify the `application.conf` file and set the configuration to bypass auth and use your own API keys. 

## Running the Server
To run the server, run `sbt run` from the project root. This will automatically serve the static files on port `8080`.

## Deploying to Heroku via CLI
This project uses the **sbt-native-packager** plugin to handle the project builds.
1. Create a new application on [Heroku](https://heroku.com/)
2. Add **Heroku Postgres** as an add-on to your app in Heroku.
3. The Heroku Postgres add-on will automatically add a **DATABASE_URL** environment variable to your app. The environment variable will automatically override the database config URL.
4. In your app settings on Heroku, go to **Settings ▸ Reveal Config Vars** and add the following environment variables:
   - `client_id` - The Client ID for your app (from BC Devtools)
   - `client_secret` - The Client Secret for your app (from BC Devtools)
   - `callback` - This should be the URL for your Heroku app's `/auth/install` route. (ex. https://your-app-name.herokuapp.com/auth/install)
   - `MODE`: `PROD` - this is to ensure that the server is not started in development mode.
5. Open a new terminal in this project's directory
6. If you've already forked the repository, create the remote by running `heroku git:remote -a your-heroku-app-name`
7. Run `git push heroku master` to compile and build the app on Heroku.
