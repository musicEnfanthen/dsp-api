<!---
Copyright © 2015-2019 the contributors (see Contributors.md).

This file is part of Knora.

Knora is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Knora is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public
License along with Knora.  If not, see <http://www.gnu.org/licenses/>.
-->

# Client API Code Generation Framework

@@toc

## Requirements

* Simplify the development of clients that work with Knora APIs.
* Reduce the need for manual changes in client code when Knora APIs change.
* At minimum, generate client API code in
    * TypeScript
    * Python
* Generate:
    * Endpoint definitions containing function definitions in the target language.
    * Class definitions corresponding to the built-in classes that Knora uses in its APIs.
* Include client function definitions in Knora route definitions.

In the future, it would also be useful to generate project-specific client
APIs, with class definitions corresponding to project-specific classes.
  
## Implementation

Client APIs are defined in Scala and extend the `ClientApi` trait. There
is currently an implementation for the admin API, called `AdminClientApi`.
A `ClientApi` contains one or more `KnoraRoute` implementations that extend
`ClientEndpoint`. Each endpoint defines functions to be generated for performing
API operations that use the route.

The route `ClientApiRoute` generates all available client APIs for a specified
target, returning source code in a Zip file. For instructions on using
this route, see
@ref:[Generating Client API Code](../../development/generating-client-apis.md).

This route has a front end, `GeneratorFrontEnd`, which that gets API class
definitions from `OntologyResponderV2` and transforms them into a data structure
that is suitable for code generation. The route supports different back ends for
different targets. A back end determines which files need to be generated,
generates each file using a Twirl template, and arranges the files in the
correct directory structure.

Currently one back end, `TypeScriptBackEnd`, is implemented; it generates code
for use with [knora-api-js-lib](https://github.com/dhlab-basel/knora-api-js-lib).

## Client Function DSL

Client function definitions are written in a Scala DSL. A function definition
looks like this:

@@snip [UsersRouteADM.scala]($src$/org/knora/webapi/routing/admin/UsersRouteADM.scala) { #getUserGroupMembershipsFunction }

The `description` keyword specifies a documentation comment describing the function.
A function has `params`, each of which also has a `description`, as well as a `paramType`.
Built-in types are defined in `ClientApi.scala` and extend `ClientObjectType`.
Class types can be constructed using the `classRef` function, as shown above.
If a parameter is optional, use `paramOptionType` instead of `paramType`.

The `doThis` keyword introduces the body of a function, which can be either
an HTTP operation or a function call. After the `doThis` block, `returns`
specifies the return type of the function.

### HTTP Operations

An HTTP operation is introduced by `httpGet`, `httpPost`, `httpPut`, or
`httpDelete`; it takes a `path` and (if it `httpPost` or `httpPut`) an optional
request body. The path consists of elements separated by slashes. Each element
is either `str()` representing a string literal, `arg` representing an argument
that was passed to the function, or `argMember()` representing a member of an
argument.

URL parameters can be added like this:

@@snip [PermissionsRouteADM.scala]($src$/org/knora/webapi/routing/admin/PermissionsRouteADM.scala) { #getAdministrativePermissionFunction }

Here is an example with a request body:

@@snip [UsersRouteADM.scala]($src$/org/knora/webapi/routing/admin/UsersRouteADM.scala) { #createUserFunction }

In this case, the request body is the `user` argument that was passed to the function.

The request body can also be a constructed JSON object:

@@snip [UsersRouteADM.scala]($src$/org/knora/webapi/routing/admin/UsersRouteADM.scala) { #updateUserBasicInformationFunction }

### Function Calls

Instead of performing an HTTP operation directly, a function can call another
function, like this:

@@snip [UsersRouteADM.scala]($src$/org/knora/webapi/routing/admin/UsersRouteADM.scala) { #getUserFunction }
@@snip [UsersRouteADM.scala]($src$/org/knora/webapi/routing/admin/UsersRouteADM.scala) { #getUserByIriFunction }

If an argument of the calling function needs to be converted to another type
for the function call, use the `as` keyword as shown above.