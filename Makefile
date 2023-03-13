STATIC=public/*.html public/*

all: build build/server.js

build/server.js: src/**/* shadow-cljs.edn node_modules
	npx shadow-cljs release server sw core

build: src/**/* $(STATIC)
	mkdir -p build/public
	cp -LR --preserve=all $(STATIC) build/public
	npx shadow-cljs release sw core
	touch build

node_modules: package.json
	npm i
	touch node_modules

.PHONY: install watch watcher server repl clean

server: node_modules
	@echo "waiting for devserver.js to appear."
	@rm -f devserver.js; until [ -f devserver.js -a -d .shadow-cljs ]; do sleep 1; done; echo "devserver.js appeared. starting."
	@sleep 1 && while [ 1 ]; do DEV=1 node devserver.js; sleep 3; echo "restarting devserver.js"; done

watcher:
	npx shadow-cljs watch server repl sw core

watch:
	make -j2 watcher server

install:
	./install_clojure.sh
	export PATH="./clj/bin:$PATH"

repl:
	npx shadow-cljs cljs-repl app

clean:
	rm -rf .shadow-cljs devserver.js server.js build node_modules package-lock.json
