((web-mode . ((eval . (progn
                        (setq-local web-mode-engines-alist '(("liquid" . "\\.html\\'")))
                        (put 'web-mode-engines-alist 'permanent-local t)
                        (web-mode))))))
