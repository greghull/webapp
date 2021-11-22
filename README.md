# webapp

A Clojure web framework for getting sites up and running quickly.

Will Feature:

- User authentication
- Stripe Payments
- Email 
- Document-based Database
- Basic Administration

## Usage

$ lein uberjar
$ java -jar <path to webapp-standalone jar>

Or run it from your repl by calling (mount/start) in webapp.core

## License

Copyright © 2021 Greg Hull

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
