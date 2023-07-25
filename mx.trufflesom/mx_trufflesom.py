import os
from mx import GitConfig


# ensure the core-lib is present
if not os.path.exists('core-lib/.git'):
  git = GitConfig()
  git.run(['git', 'submodules', 'update', '--init', '--recursive'])
