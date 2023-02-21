# rewrite-arrow

Spike to investigate automating migration from Arrow 1.1.x to Arrow 1.2.x.
The goal is to automate fixing `@Deprecated` code, such that the resulting (non-deprecated) code is source -and binary compatible with Arrow 2.0.0.

# Tasks

- [ ] Rewrite imports for `arrow.core.computations.*` to `arrow.core.raise.*`
 - [ ] Add missing `ensure` import based on usage
 
- [ ] Rewrite imports for `arrow.core.continuations.*` to `arrow.core.raise.*` 
 - [ ] Add missing `ensure` import based on usage
 - [ ] Add missing `Effect.xxx` or `EagerEffect.xx` import based on usage (`fold`, `toEither`, etc).
 
 - [ ] Migrate `Validated` to `Either`
 
 - [ ] Write migration guide using OpenRewrite
 
