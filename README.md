# ops-panel

Experiments in visualization and web-based ops helpers.

## Play with it

- [Install boot](https://github.com/boot-clj/boot#install),

- Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` environment variables to the values found in [this test github app](https://github.com/settings/applications/346358).

- clone this repo,

- `cd` to its root directory,

- run `boot dev`,

- browse to http://127.0.0.1:3000/ (actually use `127.0.0.1` and not `localhost` or other alias.  You'll trigger a CSRF error the first time you log in otherwise, because the abovementioned github app is configured with a callback address using `127.0.0.1`.)

- connect to local [nRepl](https://github.com/clojure/tools.nrepl) via [cider](https://github.com/clojure-emacs/cider) (if using emacs) or by opening `boot repl -c` from a different shell window.

- Server-side logic goes in `src/clj/ops_panel`.  To see the effect of server-side changes, save the relevant `.clj` files and reload the page,

- Client-side logic is in `src/cljs/ops_panel`.  Static assets are in `/res/public`.  Changes in both should be shown on save; no page reload necessary.

An [http-kit](http://www.http-kit.org/) server is used for development.  This is because it's the only option supported natively by both [sente](https://github.com/ptaoussanis/sente) and [boot-http](https://github.com/pandeiro/boot-http).

If you're interested in dev tooling setup, [this tutorial](https://github.com/magomimmo/modern-cljs/blob/master/doc/second-edition/tutorial-01.md) is a more leisurely introduction to boot and related tools.

## Deploying

This code is deployed to [`ops.lantern.io`](http://ops.lantern.io).

### tl;dr

```bash
ssh lantern@ops.lantern.io
cd ops-panel
git pull
script/deploy.sh
```

### Details

The production deployment uses [nginx-clojure](https://github.com/nginx-clojure/nginx-clojure) as the web server.  This was chosen for having native support for TLS and for sente[1].

Our deployment of nginx-clojure was set up manually, roughly following [these steps](https://github.com/nginx-clojure/nginx-clojure/tree/master/example-projects/clojure-web-example).  Since this is a singleton machine, I didn't bother making a Salt configuration for this.  The only custom parts are the files in [`etc`](https://github.com/getlantern/ops-panel/tree/master/etc).  The (so far unoptimized) configuration for the web server is in [`etc/nginx.conf`](https://github.com/getlantern/ops-panel/blob/master/etc/nginx.conf).  The upstart configuration for this server is in [`etc/upstart_conf`](https://github.com/getlantern/ops-panel/blob/master/etc/upstart_conf).  Environment variables are edited directly in `/etc/environment` at the production machine.

To create an [uberjar](http://stackoverflow.com/questions/11947037/what-is-an-uber-jar) for deployment run `boot build`.  This will create a `target/project.jar` which should replace `/opt/nginx-clojure-0.4.4/libs/ops-panel.jar`.  Since uploading this file may be onerous, a clone of this repo is checked out in `/home/lantern/ops-panel` in the production machine, so you can build it there and then copy it (again, `script/deploy.sh` does that).

[1] [P.S (aranhoide)]: Later @uaalto told me that Immutant 2 met these conditions too, but nothing compels me to switch so far.
