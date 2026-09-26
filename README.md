# SFA

Experimental Android client for sing-box, the universal proxy platform.

## Documentation

https://sing-box.sagernet.org/installation/clients/sfa/

## Android CI

Pushes to `main` build only the modern Android variant for ARMv7, with generated
app versions, native-library caching, lint checks and a downloadable APK.
Pull requests, tag pushes and bot branch pushes do not trigger this build.
Manual runs remain available, and only one Android CI run is kept active.
Other ABIs remain configurable; the legacy APK job is disabled.
Release signing is optional and restricted to the default branch.

See [CI setup and versioning (Türkçe)](docs/CI.md) for signing secrets, versioning
rules, download instructions and the project/UI analysis.

## License

```
Copyright (C) 2022 by nekohasekai <contact-sagernet@sekai.icu>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.

In addition, no derivative work may use the name or imply association
with this application without prior consent.
```

Under the license, that forks of the app are not allowed to be listed on F-Droid or other app stores
under the original name.
