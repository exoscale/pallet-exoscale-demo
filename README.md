pallet-exoscale-demo: simple infrastructure with pallet
=======================================================

Deploy a test infrastructure on pallet.

### Application:

The demo app is a simple yet usefull URL-Shortener. This shortener is a 3 tier application with the following layers:
- Load Balancer: NGINX
- Web Server: Python
- Database Server: Redis

It will make a store a hash of your long url to a exo.po/XXXXX patern 


### Deployer

To make things easier and to hide the pallet magic, we have created a web deployer, 
which is an web application that will control the URL-Shortener deployement and configuration. Once you run the project,
the deployer will be built and present you with the following interface:

![exoscale deployer](http://i.imgur.com/KKcoyDX.png)

### Use Case

Once running, you can deploy the URL-Shortener with:
- 1 LB
- 1 WEB
- 1 DB

Then to scale up for more trafic, you can specify:
- 1 LB
- 3 WEB
- 1 DB

Pallet will then spawn 2 new webserver instances, intall packages and URL-Shortener code and eventually reconfigure the NGINX 
LB to take in account the 2 new workers.

```
root@lb-ac:~# cat /etc/nginx/conf.d/upstream-shorten.conf
upstream shorten {
	server 185.19.28.65:8080;
}

root@lb-ac:~# cat /etc/nginx/conf.d/upstream-shorten.conf
upstream shorten {
	server 185.19.28.83:8080;
	server 185.19.28.86:8080;
	server 185.19.28.65:8080;
}
```

## Prerequisites

To run the demo, you will need:
- A working JVM environment
- The Leiningen http://leiningen.org/#install
- A valid exoscale account

## Configuration

You will need an OpenSSH format RSA key in `$HOME/.ssh/id_rsa`.
Add the following in `$HOME/.pallet/services/exoscale.clj`:

```clojure
{:exoscale {:provider "exoscale"
            :api-key "<API_KEY>"
			:api-secret "<API_SECRET>"}}
```

## Running

Simply run `lein run` in the project's directory and point
your browser to http://localhost:8080


