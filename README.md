pallet-exoscale-demo: simple infrastructure with pallet
=======================================================

Deploy a test infrastructure on pallet.

![exoscale deployer](http://i.imgur.com/KKcoyDX.png)

## Configuration

You will need an OpenSSH format RSA key in `$HOME/.ssh/id_rsa`.
Add the following in `$HOME/.pallet/services/exoscale.clj`:

```clojure
{:exoscale {:provider "exoscale"
            :api-key "<API_KEY>"
			:api-secret "<API_SECRET>"}}
```

## Prerequisites

To run the demo, you will need:
- A working JVM environment
- The Leiningen http://leiningen.org/#install
- A valid exoscale account

## Running

Simply run `lein run` in the project's directory and point
your browser to http://localhost:8080


