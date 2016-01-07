# Adders / Removers generator

This plugin allows to generate adders and removers methods for class properties based on annotated type.
Recognized types are: `array`, `ClassName[]`, `Doctrine\Common\Collections\Collection`.

It is possible to generate only adders, only removers or both at the same time: plugin adds three menu items to class "Generate" context menu - https://i.imgur.com/ExPnHNN.png


*__Works in PhpStorm 9 and 10. May be working in other IDE versions, but this was not checked.__*

# Examples

### Before

```php
namespace App\Entity;

class Entity
{
    /**
     * @var array
     */
    protected $users;
}
```
### After

```php
namespace App\Entity;

class Entity
{
    /**
     * @var array
     */
    protected $users;

    /**
     * @param mixed $var
     */
    public function addUser($var)
    {
        $this->users[] = $var;
    }

    /**
     * @param mixed $var
     */
    public function removeUser($var)
    {
        if ($key = array_search($var, $this->users, true) !== false) {
            array_splice($this->users, $key, 1);
        }
    }
}
```
---
### Before

```php
namespace App\Entity;

class Entity
{
    /**
     * @var User[]
     */
    protected $users;
}
```

### After

```php
namespace App\Entity;

class Entity
{
    /**
     * @var User[]
     */
    protected $users;

    /**
     * @param \App\Entity\User $var
     */
    public function addUser(User $var)
    {
        $this->users[] = $var;
    }

    /**
     * @param \App\Entity\User $var
     */
    public function removeUser(User $var)
    {
        if ($key = array_search($var, $this->users, true) !== false) {
            array_splice($this->users, $key, 1);
        }
    }
}
```
---
### Before

```php
namespace App\Entity;

use Doctrine\Common\Collections\Collection;

class Entity
{
    /**
     * @var User[]|Collection
     */
    protected $users;
}
```

### After

```php
namespace App\Entity;

use Doctrine\Common\Collections\Collection;

class Entity
{
    /**
     * @var User[]|Collection
     */
    protected $users;

    /**
     * @param \App\Entity\User $var
     */
    public function addUser(User $var)
    {
        $this->users->add($var);
    }

    /**
     * @param \App\Entity\User $var
     */
    public function removeUser(User $var)
    {
        $this->users->removeElement($var);
    }
}
```
