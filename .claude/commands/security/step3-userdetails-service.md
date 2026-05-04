Build the CustomUserDetailsService for NotePlus.

Read CLAUDE.md. Read the User and Role entities in src/main/java/org/noteplus/noteplus/entity/.

First, create the UserRepository if it does not exist:
`src/main/java/org/noteplus/noteplus/repository/UserRepository.java`
- Extends JpaRepository<User, UUID>
- Add: Optional<User> findByUsername(String username);
- Add: boolean existsByUsername(String username);
- Add: boolean existsByEmail(String email);

Also create RoleRepository:
- JpaRepository<Role, UUID>
- Optional<Role> findByName(String name);

Then create `src/main/java/org/noteplus/noteplus/security/CustomUserDetailsService.java`:
- @Service @RequiredArgsConstructor
- Implements UserDetailsService
- Inject UserRepository
- Override loadUserByUsername(String username):
    - Find user by username, throw UsernameNotFoundException("Invalid credentials") if absent — NOT "User not found" (prevents username enumeration)
    - Map Role entities to GrantedAuthority using role.getName() (already has ROLE_ prefix)
    - Return new org.springframework.security.core.userdetails.User(username, password, authorities)