# ops-panel

Experiments in visualization and web-based ops helpers.

## Play with it

- [Install boot](https://github.com/boot-clj/boot#install),

- clone this repo,

- `cd` to its root directory,

- run `boot dev`,

- browse to http://localhost:3000/

- connect to local [nRepl](https://github.com/clojure/tools.nrepl) via [cider](https://github.com/clojure-emacs/cider) (if using emacs) or by opening `boot repl -c` from a different shell window.

- Server-side logic goes in `src/clj/ops_panel`.  To see the effect of server-side changes, save the relevant `.clj` files and reload the page,

- Client-side logic is in `src/cljs/ops_panel`.  Static assets are in `/res/public`.  Changes in both should be shown on save; no page reload necessary.

If you're interested in dev tooling setup, [this tutorial](https://github.com/magomimmo/modern-cljs/blob/master/doc/second-edition/tutorial-01.md) is a more leisurely introduction to boot and related tools.
