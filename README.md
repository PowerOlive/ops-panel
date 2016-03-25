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

- Changes to Clojurescript and to static assets (which put in `/html`) should be shown on save; no page reload necessary.

If you're interested in this kind of stuff, [this tutorial](https://github.com/magomimmo/modern-cljs/blob/master/doc/second-edition/tutorial-01.md) is a more leisurely introduction to boot and related tools.
