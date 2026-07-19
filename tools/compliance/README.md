# Compliance Tools

Repository compliance scripts turn documented policy into repeatable checks.

`check-licenses.mjs` reads pnpm's resolved dependency inventory and rejects licenses that require
explicit legal approval under the Phase P1 dependency policy. The generated inventory remains the
review evidence; this automated denylist does not replace legal review.
